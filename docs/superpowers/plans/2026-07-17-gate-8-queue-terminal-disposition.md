# Gate 8 Durable Queue Terminal Disposition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Gate 8 Scheduler queue an explicit terminal disposition so a failed work item is recorded distinctly from a verified completion and never satisfies its dependents.

**Architecture:** Add a `WorkItemDisposition` enum and a second terminal set (`failedWorkItemIds`) to the immutable `SchedulerQueueState`, extend the partition invariant, split the single `completeActive` operation into `completeActiveVerified` / `failActive` across the in-memory, durable, and filesystem layers, and revise the unreleased schema-v1 on-disk format in place. Verification-driven disposition recording and result/RunRecord wiring stay out of scope.

**Tech Stack:** Java 17, JUnit 5, Gradle (via `scripts/gradle.ps1`), no new dependencies.

## Global Constraints

- Java 17; production must compile clean under `-Xlint:all -Werror` (strict lint).
- `CURRENT_SCHEMA_VERSION` stays `1`. No schema version bump, no migration code.
- The on-disk queue format has no forward-compat marker; adding a field makes existing local `.enhancer/` snapshots fail closed as `CorruptedSchedulerQueueStateException`. Acceptable — unreleased local dev artifact — and must be recorded as an accepted decision (Task 6).
- `completedWorkItemIds` keeps its exact meaning: verified-completed = dependency-satisfying. Failed work is a separate set and never enters the dependency-satisfaction check.
- Preserve every existing public invariant, enum constant name, envelope/WorkItem provenance, and the persist-before-expose ordering (`store.update` before exposing new in-memory state).
- The queue stores disposition only — never a failure reason/detail (that lives in the runtime/RunRecord, linked by `workItemId`).
- Out of scope: `ResultPayload`/RunRecord wiring, dispatcher-driven disposition recording, retry, automatic failure propagation, a non-terminal awaiting-verification queue state (Option B), workers, effect fencing.
- Per `.ai/workflow.md:27` and the session handoff, **do not commit or push without explicit user authorization.** Commit steps below are prepared but must only run once the user has given the go-ahead.
- Classify every RED before implementing (per `.ai/workflow.md` step 6): the failure must be caused only by the missing behavior the test targets, matching this plan and accepted decisions. A missing symbol is valid RED evidence.

**Build commands (PowerShell shell):**
- Focused package: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.*'`
- Single suite: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.WorkItemDispositionTest'`
- Full regression: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`

---

## File Structure

- Create: `src/main/java/com/enhancer/runtime/WorkItemDisposition.java` — the terminal disposition enum.
- Modify: `src/main/java/com/enhancer/runtime/SchedulerQueueState.java` — add `failedWorkItemIds`, extend partition invariant.
- Modify: `src/main/java/com/enhancer/runtime/SingleWorkerSchedulerQueue.java` — rename `completeActive` → `completeActiveVerified`, add `failActive`, `failedWorkItemIds()`, `dispositionOf`, carry failed set through snapshot/rehydrate.
- Modify: `src/main/java/com/enhancer/runtime/FileSystemSchedulerQueueStore.java` — serialize/deserialize `failedWorkItemIds`.
- Modify: `src/main/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueue.java` — rename delegating method, add `failActive`, `failedWorkItemIds()`.
- Create: `src/test/java/com/enhancer/runtime/WorkItemDispositionTest.java`
- Create: `src/test/java/com/enhancer/runtime/SchedulerQueueStateTest.java`
- Modify tests: `SingleWorkerSchedulerQueueTest.java`, `DurableSingleWorkerSchedulerQueueTest.java`, `FileSystemSchedulerQueueStoreIntegrationTest.java`
- Modify docs (Task 6): `ARCHITECTURE.md`, `.ai/architecture.md`, `DECISION_LOG.md`, `CURRENT_TASK.md`, `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, `CHANGELOG.md`.

---

### Task 1: `WorkItemDisposition` enum

**Files:**
- Create: `src/main/java/com/enhancer/runtime/WorkItemDisposition.java`
- Test: `src/test/java/com/enhancer/runtime/WorkItemDispositionTest.java`

**Interfaces:**
- Produces: `enum WorkItemDisposition { VERIFIED_COMPLETED, FAILED }` with `boolean satisfiesDependencies()`.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/enhancer/runtime/WorkItemDispositionTest.java`:

```java
package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorkItemDispositionTest {
    @Test
    void onlyVerifiedCompletionSatisfiesDependencies() {
        assertTrue(WorkItemDisposition.VERIFIED_COMPLETED.satisfiesDependencies());
        assertFalse(WorkItemDisposition.FAILED.satisfiesDependencies());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.WorkItemDispositionTest'`
Expected: compilation failure — `WorkItemDisposition` does not exist. Classify: aligned missing implementation.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/enhancer/runtime/WorkItemDisposition.java`:

```java
package com.enhancer.runtime;

/**
 * Terminal disposition of a Scheduler queue work item. Only a verified completion satisfies a
 * dependent's dependency; a failure is terminal and never satisfies dependencies.
 */
public enum WorkItemDisposition {
    VERIFIED_COMPLETED,
    FAILED;

    public boolean satisfiesDependencies() {
        return this == VERIFIED_COMPLETED;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.WorkItemDispositionTest'`
Expected: PASS.

- [ ] **Step 5: Commit** (only with user authorization)

```bash
git add src/main/java/com/enhancer/runtime/WorkItemDisposition.java src/test/java/com/enhancer/runtime/WorkItemDispositionTest.java
git commit -m "feat: add WorkItemDisposition terminal enum"
```

---

### Task 2: `SchedulerQueueState` failed-disposition set and partition invariant

**Files:**
- Modify: `src/main/java/com/enhancer/runtime/SchedulerQueueState.java`
- Modify (to keep the module compiling): `src/main/java/com/enhancer/runtime/SingleWorkerSchedulerQueue.java:132-143` and `src/main/java/com/enhancer/runtime/FileSystemSchedulerQueueStore.java:290-299`
- Test: `src/test/java/com/enhancer/runtime/SchedulerQueueStateTest.java`

**Interfaces:**
- Consumes: existing package-private `SchedulerQueueState(int, String, long, int, Optional<String>, List<String>, List<QueuedWork>, Optional<QueuedWork>, Set<String>)`.
- Produces: constructor gains a trailing `Set<String> failedWorkItemIds` parameter; new accessor `Set<String> failedWorkItemIds()`. `initial(...)` signature unchanged.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/enhancer/runtime/SchedulerQueueStateTest.java`:

```java
package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerQueueStateTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000301";
    private static final String FIRST_ID =
            "00000000-0000-0000-0000-000000000311";
    private static final String SECOND_ID =
            "00000000-0000-0000-0000-000000000312";

    @Test
    void partitionsAdmittedWorkAcrossVerifiedAndFailedDispositions() {
        SchedulerQueueState state = new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID, SECOND_ID),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(SECOND_ID));

        assertEquals(Set.of(FIRST_ID), state.completedWorkItemIds());
        assertEquals(Set.of(SECOND_ID), state.failedWorkItemIds());
    }

    @Test
    void rejectsWorkItemThatIsBothCompletedAndFailed() {
        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(FIRST_ID)));
    }

    @Test
    void rejectsFailedWorkThatWasNeverAdmitted() {
        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(SECOND_ID)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.SchedulerQueueStateTest'`
Expected: compilation failure — constructor has no failed-set parameter and `failedWorkItemIds()` does not exist. Classify: aligned missing implementation.

- [ ] **Step 3: Add the field, parameter, and accessor**

In `SchedulerQueueState.java`, add the field after `completedWorkItemIds` (near line 28):

```java
    private final Set<String> completedWorkItemIds;
    private final Set<String> failedWorkItemIds;
```

Change the constructor signature (line 30-39) to add the trailing parameter:

```java
    SchedulerQueueState(
            int schemaVersion,
            String queueId,
            long revision,
            int maxWorkItems,
            Optional<String> logicalRunId,
            List<String> admissionOrder,
            List<QueuedWork> pendingWork,
            Optional<QueuedWork> activeWork,
            Set<String> completedWorkItemIds,
            Set<String> failedWorkItemIds) {
```

Assign the field right after the existing `completedWorkItemIds` assignment (line 76-79), before `validateStructure()`:

```java
        this.completedWorkItemIds = canonicalIdentitySet(
                completedWorkItemIds,
                "completedWorkItemIds",
                maxWorkItems);
        this.failedWorkItemIds = canonicalIdentitySet(
                failedWorkItemIds,
                "failedWorkItemIds",
                maxWorkItems);
        validateStructure();
```

Add the accessor after `completedWorkItemIds()` (line 130-132):

```java
    public Set<String> failedWorkItemIds() {
        return failedWorkItemIds;
    }
```

- [ ] **Step 4: Extend the partition invariant**

In `validateStructure()`, immediately after the existing loop that adds completed ids to `statusIds` (line 155-159), add the failed loop with a disjointness check:

```java
        Set<String> statusIds = new LinkedHashSet<>();
        for (String completed : completedWorkItemIds) {
            requireAdmitted(positions, completed);
            statusIds.add(completed);
        }
        for (String failed : failedWorkItemIds) {
            requireAdmitted(positions, failed);
            if (!statusIds.add(failed)) {
                throw new IllegalArgumentException(
                        "work item must not be both completed and failed");
            }
        }
```

The remaining pending/active accumulation and the `statusIds.equals(admissionOrder)` partition check are unchanged; failed ids now participate in the partition. The active-dependency check keeps referencing `completedWorkItemIds` only (line 173) — a failed dependency therefore never satisfies dependents.

- [ ] **Step 5: Update `initial(...)` and the two other constructor callers to compile**

In `SchedulerQueueState.initial(...)` (line 86-95), pass an empty failed set as the new last argument:

```java
        return new SchedulerQueueState(
                CURRENT_SCHEMA_VERSION,
                queueId,
                0,
                maxWorkItems,
                Optional.empty(),
                List.of(),
                List.of(),
                Optional.empty(),
                Set.of(),
                Set.of());
```

In `SingleWorkerSchedulerQueue.java` `snapshot(...)` (line 132-143), pass an empty failed set for now (Task 3 replaces it with the real field):

```java
    SchedulerQueueState snapshot(String queueId, long revision) {
        return new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                queueId,
                revision,
                maxWorkItems,
                logicalRunId(),
                List.copyOf(admittedWorkItemIds),
                List.copyOf(pending.values()),
                Optional.ofNullable(active),
                completedWorkItemIds,
                Set.of());
    }
```

In `FileSystemSchedulerQueueStore.java` `decode(...)` (line 290-299), pass an empty failed set for now (Task 4 replaces it with the deserialized set):

```java
            return new SchedulerQueueState(
                    schemaVersion,
                    queueId,
                    revision,
                    maxWorkItems,
                    logicalRunId,
                    admissionOrder,
                    pendingWork,
                    activeWork,
                    completedWorkItemIds,
                    Set.of());
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.*'`
Expected: PASS, including the new `SchedulerQueueStateTest` and all existing runtime tests (failed set is empty everywhere, so the partition is unchanged for existing flows).

- [ ] **Step 7: Commit** (only with user authorization)

```bash
git add src/main/java/com/enhancer/runtime/SchedulerQueueState.java src/main/java/com/enhancer/runtime/SingleWorkerSchedulerQueue.java src/main/java/com/enhancer/runtime/FileSystemSchedulerQueueStore.java src/test/java/com/enhancer/runtime/SchedulerQueueStateTest.java
git commit -m "feat: add failed-disposition set to scheduler queue state"
```

---

### Task 3: In-memory queue disposition API (`failActive` + rename)

**Files:**
- Modify: `src/main/java/com/enhancer/runtime/SingleWorkerSchedulerQueue.java`
- Modify (rename cascade): `src/main/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueue.java:82-86`
- Modify tests (rename cascade + new coverage): `src/test/java/com/enhancer/runtime/SingleWorkerSchedulerQueueTest.java`, `src/test/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueueTest.java`, `src/test/java/com/enhancer/runtime/FileSystemSchedulerQueueStoreIntegrationTest.java`

**Interfaces:**
- Consumes: `WorkItemDisposition` (Task 1), `SchedulerQueueState.failedWorkItemIds()` (Task 2).
- Produces on `SingleWorkerSchedulerQueue`: `void completeActiveVerified(String)` (was `completeActive`), `void failActive(String)`, `Set<String> failedWorkItemIds()`, `Optional<WorkItemDisposition> dispositionOf(String)`. On `DurableSingleWorkerSchedulerQueue`: `void completeActiveVerified(String)` (was `completeActive`).

- [ ] **Step 1: Write the failing tests**

Add these two tests to `SingleWorkerSchedulerQueueTest.java` (import `WorkItemDisposition` is same-package, no import needed):

```java
    @Test
    void failedWorkNeverSatisfiesDependentsAndRecordsFailedDisposition() {
        WorkItem first = workItem(FIRST_ID, "read-file-worker");
        WorkItem second = workItem(SECOND_ID, "review-worker");
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue();

        queue.enqueue(new QueuedWork(first, List.of()));
        queue.enqueue(new QueuedWork(second, List.of(FIRST_ID)));

        assertSame(first, queue.claimNext().orElseThrow());
        queue.failActive(FIRST_ID);

        assertTrue(queue.claimNext().isEmpty());
        assertTrue(queue.activeWork().isEmpty());
        assertEquals(Set.of(), queue.completedWorkItemIds());
        assertEquals(Set.of(FIRST_ID), queue.failedWorkItemIds());
        assertEquals(Optional.of(WorkItemDisposition.FAILED),
                queue.dispositionOf(FIRST_ID));
        assertEquals(Optional.empty(), queue.dispositionOf(SECOND_ID));
        assertThrows(IllegalStateException.class, () -> queue.failActive(FIRST_ID));
        assertThrows(NullPointerException.class, () -> queue.failActive(null));
    }

    @Test
    void verifiedCompletionRecordsVerifiedDispositionAndReleasesDependents() {
        WorkItem first = workItem(FIRST_ID, "read-file-worker");
        WorkItem second = workItem(SECOND_ID, "review-worker");
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue();

        queue.enqueue(new QueuedWork(first, List.of()));
        queue.enqueue(new QueuedWork(second, List.of(FIRST_ID)));

        assertSame(first, queue.claimNext().orElseThrow());
        queue.completeActiveVerified(FIRST_ID);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                queue.dispositionOf(FIRST_ID));
        assertSame(second, queue.claimNext().orElseThrow());
    }
```

Then rename every existing `completeActive(` call to `completeActiveVerified(` in `SingleWorkerSchedulerQueueTest.java` (lines 42, 44, 46, 48, 73, 109, 110).

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.SingleWorkerSchedulerQueueTest'`
Expected: compilation failure — `completeActiveVerified`, `failActive`, `failedWorkItemIds`, `dispositionOf` do not exist. Classify: aligned missing implementation.

- [ ] **Step 3: Implement the in-memory disposition API**

In `SingleWorkerSchedulerQueue.java` add the failed field after `completedWorkItemIds` (line 22):

```java
    private final Set<String> completedWorkItemIds = new LinkedHashSet<>();
    private final Set<String> failedWorkItemIds = new LinkedHashSet<>();
```

Load it in the state-rehydrating constructor after line 48-49:

```java
        this.completedWorkItemIds.addAll(
                state.completedWorkItemIds());
        this.failedWorkItemIds.addAll(
                state.failedWorkItemIds());
```

Rename `completeActive` (line 101) to `completeActiveVerified` (body unchanged) and add `failActive` plus accessors right after it:

```java
    public void completeActiveVerified(String workItemId) {
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        if (active == null) {
            throw new IllegalStateException("no active work item exists");
        }
        if (!active.workItem().workItemId().equals(workItemId)) {
            throw new IllegalStateException(
                    "only the active work item may be completed");
        }
        completedWorkItemIds.add(workItemId);
        active = null;
    }

    public void failActive(String workItemId) {
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        if (active == null) {
            throw new IllegalStateException("no active work item exists");
        }
        if (!active.workItem().workItemId().equals(workItemId)) {
            throw new IllegalStateException(
                    "only the active work item may be failed");
        }
        failedWorkItemIds.add(workItemId);
        active = null;
    }

    public Set<String> failedWorkItemIds() {
        return Set.copyOf(failedWorkItemIds);
    }

    public Optional<WorkItemDisposition> dispositionOf(String workItemId) {
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        if (completedWorkItemIds.contains(workItemId)) {
            return Optional.of(WorkItemDisposition.VERIFIED_COMPLETED);
        }
        if (failedWorkItemIds.contains(workItemId)) {
            return Optional.of(WorkItemDisposition.FAILED);
        }
        return Optional.empty();
    }
```

Update `snapshot(...)` (changed in Task 2) to pass the real failed set as the last argument instead of `Set.of()`:

```java
                Optional.ofNullable(active),
                completedWorkItemIds,
                failedWorkItemIds);
```

- [ ] **Step 4: Rename the delegating method in the durable wrapper**

In `DurableSingleWorkerSchedulerQueue.java` (line 82-86) rename the method and its delegate call:

```java
    public void completeActiveVerified(String workItemId) throws IOException {
        SingleWorkerSchedulerQueue candidate = copyQueue();
        candidate.completeActiveVerified(workItemId);
        adoptAfterPersistence(candidate);
    }
```

Rename the callers in `DurableSingleWorkerSchedulerQueueTest.java` (lines 50, 84) and `FileSystemSchedulerQueueStoreIntegrationTest.java` (line 85) from `completeActive(` to `completeActiveVerified(`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.*'`
Expected: PASS, including the two new in-memory tests and all renamed existing tests.

- [ ] **Step 6: Commit** (only with user authorization)

```bash
git add src/main/java/com/enhancer/runtime/SingleWorkerSchedulerQueue.java src/main/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueue.java src/test/java/com/enhancer/runtime/SingleWorkerSchedulerQueueTest.java src/test/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueueTest.java src/test/java/com/enhancer/runtime/FileSystemSchedulerQueueStoreIntegrationTest.java
git commit -m "feat: split scheduler queue completion into verified and failed dispositions"
```

---

### Task 4: Serialize the failed set in the filesystem store

**Files:**
- Modify: `src/main/java/com/enhancer/runtime/FileSystemSchedulerQueueStore.java` (`encode` line 237-252, `decode` line 254-313)
- Test: `src/test/java/com/enhancer/runtime/FileSystemSchedulerQueueStoreIntegrationTest.java`

**Interfaces:**
- Consumes: `SchedulerQueueState.failedWorkItemIds()` (Task 2), `SingleWorkerSchedulerQueue.failActive` / `snapshot` (Task 3).
- Produces: schema-v1 on-disk payload now carries the failed set as a trailing string set; `decode` reconstructs it. No public signature change.

- [ ] **Step 1: Write the failing test**

Add to `FileSystemSchedulerQueueStoreIntegrationTest.java`:

```java
    @Test
    void roundTripsFailedDispositionAcrossStoreInstances() throws Exception {
        String firstId =
                "00000000-0000-0000-0000-000000000321";
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        store.create(SchedulerQueueState.initial(QUEUE_ID, 8));

        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue(8);
        queue.enqueue(new QueuedWork(workItem(firstId, "reader"), List.of()));
        queue.claimNext().orElseThrow();
        queue.failActive(firstId);
        store.update(queue.snapshot(QUEUE_ID, 1));

        SchedulerQueueState resolved =
                new FileSystemSchedulerQueueStore(storageRoot).resolve(QUEUE_ID);
        assertEquals(Set.of(firstId), resolved.failedWorkItemIds());
        assertEquals(Set.of(), resolved.completedWorkItemIds());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.FileSystemSchedulerQueueStoreIntegrationTest'`
Expected: FAIL — `resolved.failedWorkItemIds()` is empty because `encode`/`decode` drop the failed set (decode still passes `Set.of()`). Classify: aligned missing implementation.

- [ ] **Step 3: Serialize and deserialize the failed set**

In `encode(...)` add the failed set write immediately after the completed set write (line 249):

```java
            writeStringSet(output, state.completedWorkItemIds());
            writeStringSet(output, state.failedWorkItemIds());
```

In `decode(...)` read the failed set immediately after the completed set read (line 284), before the trailing-bytes check (line 285), and pass it to the constructor (replacing the `Set.of()` placeholder from Task 2):

```java
            Set<String> completedWorkItemIds = readStringSet(input);
            Set<String> failedWorkItemIds = readStringSet(input);
            if (input.available() != 0) {
                throw corrupted(
                        expectedQueueId,
                        "state contains trailing bytes");
            }
            return new SchedulerQueueState(
                    schemaVersion,
                    queueId,
                    revision,
                    maxWorkItems,
                    logicalRunId,
                    admissionOrder,
                    pendingWork,
                    activeWork,
                    completedWorkItemIds,
                    failedWorkItemIds);
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.FileSystemSchedulerQueueStoreIntegrationTest'`
Expected: PASS. The existing corrupt/trailing/unsupported/oversized tests still pass (they never assert an exact payload length and the digest still covers the whole payload).

- [ ] **Step 5: Commit** (only with user authorization)

```bash
git add src/main/java/com/enhancer/runtime/FileSystemSchedulerQueueStore.java src/test/java/com/enhancer/runtime/FileSystemSchedulerQueueStoreIntegrationTest.java
git commit -m "feat: persist failed disposition in schema-v1 scheduler queue format"
```

---

### Task 5: Durable `failActive` with persist-before-expose and restart recovery

**Files:**
- Modify: `src/main/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueue.java`
- Test: `src/test/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueueTest.java`, `src/test/java/com/enhancer/runtime/FileSystemSchedulerQueueStoreIntegrationTest.java`

**Interfaces:**
- Consumes: `SingleWorkerSchedulerQueue.failActive` / `failedWorkItemIds()` (Task 3), the filesystem serialization (Task 4), the existing `MemoryQueueStore` test double.
- Produces on `DurableSingleWorkerSchedulerQueue`: `void failActive(String) throws IOException`, `Set<String> failedWorkItemIds()`.

- [ ] **Step 1: Write the failing tests**

Add to `DurableSingleWorkerSchedulerQueueTest.java` (uses the existing in-file `MemoryQueueStore`):

```java
    @Test
    void persistsFailedDispositionAndBlocksDependentsAcrossRecovery()
            throws Exception {
        MemoryQueueStore store = new MemoryQueueStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        WorkItem first = workItem(FIRST_ID, "read-file-worker");
        WorkItem second = workItem(SECOND_ID, "review-worker");

        queue.enqueue(new QueuedWork(first, List.of()));
        queue.enqueue(new QueuedWork(second, List.of(FIRST_ID)));
        assertSame(first, queue.claimNext().orElseThrow());
        queue.failActive(FIRST_ID);

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, store);
        assertEquals(Set.of(FIRST_ID), recovered.failedWorkItemIds());
        assertEquals(Set.of(), recovered.completedWorkItemIds());
        assertTrue(recovered.claimNext().isEmpty());
        assertTrue(recovered.activeWork().isEmpty());
    }

    @Test
    void failurePersistenceFailureLeavesTheActiveWorkVisible()
            throws Exception {
        MemoryQueueStore store = new MemoryQueueStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        WorkItem first = workItem(FIRST_ID, "read-file-worker");

        queue.enqueue(new QueuedWork(first, List.of()));
        assertSame(first, queue.claimNext().orElseThrow());
        store.failNextUpdate();
        assertThrows(IOException.class, () -> queue.failActive(FIRST_ID));

        assertEquals(Optional.of(first), queue.activeWork());
        assertTrue(queue.failedWorkItemIds().isEmpty());
    }
```

Add to `FileSystemSchedulerQueueStoreIntegrationTest.java` a real-filesystem failed round-trip through the durable wrapper:

```java
    @Test
    void recoversFailedDispositionFromTheFilesystem() throws Exception {
        String firstId =
                "00000000-0000-0000-0000-000000000331";
        String secondId =
                "00000000-0000-0000-0000-000000000332";
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        queue.enqueue(new QueuedWork(workItem(firstId, "reader"), List.of()));
        queue.enqueue(new QueuedWork(
                workItem(secondId, "reviewer"), List.of(firstId)));
        queue.claimNext().orElseThrow();
        queue.failActive(firstId);

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(storageRoot));
        assertEquals(Set.of(firstId), recovered.failedWorkItemIds());
        assertTrue(recovered.claimNext().isEmpty());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.DurableSingleWorkerSchedulerQueueTest'`
Expected: compilation failure — `DurableSingleWorkerSchedulerQueue.failActive` / `failedWorkItemIds` do not exist. Classify: aligned missing implementation.

- [ ] **Step 3: Implement durable `failActive` and accessor**

In `DurableSingleWorkerSchedulerQueue.java`, add after `completeActiveVerified` (renamed in Task 3, around line 86):

```java
    public void failActive(String workItemId) throws IOException {
        SingleWorkerSchedulerQueue candidate = copyQueue();
        candidate.failActive(workItemId);
        adoptAfterPersistence(candidate);
    }
```

Add the accessor after `completedWorkItemIds()` (line 108-110):

```java
    public Set<String> failedWorkItemIds() {
        return queue.failedWorkItemIds();
    }
```

(`copyQueue` and `adoptAfterPersistence` already give persist-before-expose and previous-revision-on-failure; no other change needed.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\scripts\gradle.ps1 --no-daemon cleanTest test --tests 'com.enhancer.runtime.*'`
Expected: PASS, including both new durable tests and the filesystem failed round-trip.

- [ ] **Step 5: Commit** (only with user authorization)

```bash
git add src/main/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueue.java src/test/java/com/enhancer/runtime/DurableSingleWorkerSchedulerQueueTest.java src/test/java/com/enhancer/runtime/FileSystemSchedulerQueueStoreIntegrationTest.java
git commit -m "feat: add durable failed-disposition recording and recovery"
```

---

### Task 6: Documentation sync, accepted decision, and full regression

**Files:**
- Modify: `DECISION_LOG.md`, `ARCHITECTURE.md`, `.ai/architecture.md`, `CURRENT_TASK.md`, `PROJECT_STATE.md`, `SESSION_HANDOFF.md`, `CHANGELOG.md`

**Interfaces:** none (documentation only). This task satisfies the project's Document Driven Development workflow (`.ai/workflow.md` steps 3-5, 10) and records the two accepted decisions the design requires.

- [ ] **Step 1: Record the accepted decision in `DECISION_LOG.md`**

Add a new dated accepted-decision entry (match the existing heading/status grammar in the file), covering:
  - The queue distinguishes verified completion from failure via a terminal `WorkItemDisposition`; `completeActiveVerified` enters the dependency-satisfaction set, `failActive` does not, so failed work leaves dependents blocked with an inspectable cause (the runtime/RunRecord holds the reason, linked by `workItemId`).
  - The active slot is kept through verification (reaffirming `DECISION_LOG.md:20`, Option A); Option B remains deferred.
  - Schema-v1 is revised in place (no version bump); because the on-disk envelope rejects trailing bytes, any pre-existing local `.enhancer/` queue snapshot **fails closed** on read, which is accepted for the unreleased local artifact.
  - Out of scope: `ResultPayload`/RunRecord wiring and dispatcher-driven disposition recording.

- [ ] **Step 2: Update `ARCHITECTURE.md` and `.ai/architecture.md`**

In the compact `.ai/architecture.md`, update the Gate 8 execution-acknowledgement bullet (currently line 32) to state that terminal queue disposition now exists: verified completion enters the dependency-satisfaction set, failure is recorded separately and never satisfies dependents, the active slot is held through verification, and result/RunRecord/dispatcher wiring remains the next connection. Mirror the same change in canonical `ARCHITECTURE.md`.

- [ ] **Step 3: Update `CURRENT_TASK.md`**

Set the task to `gate-8-durable-queue-terminal-disposition`, status `Completed` on completion, with the `Justified By` reference to the Step 1 decision, acceptance criteria mapped to the delivered contract, out-of-scope list, and the verification summary from Step 5.

- [ ] **Step 4: Update `PROJECT_STATE.md` and `SESSION_HANDOFF.md`**

Record terminal queue disposition as Contract Verified under Gate 8; keep Gate 8 the sole `Specified - Next`. In `SESSION_HANDOFF.md`, set the next task to the result-path connection (RunRecord-backed `ResultPayload` delivery and dispatcher-driven disposition recording), and note the at-least-once requeue window remains open until that increment. Add a `CHANGELOG.md` entry for the delivered contract.

- [ ] **Step 5: Run full regression and structural checks**

Run: `.\scripts\gradle.ps1 --no-daemon clean test --warning-mode all`
Expected: all suites pass (2 existing Windows symbolic-link setup skips allowed), 0 failures, 0 errors; strict lint clean.
Run: `git diff --check` (expect no whitespace errors) and confirm exactly one `Specified - Next` marker remains at Gate 8.

- [ ] **Step 6: Commit** (only with user authorization)

```bash
git add ARCHITECTURE.md .ai/architecture.md DECISION_LOG.md CURRENT_TASK.md PROJECT_STATE.md SESSION_HANDOFF.md CHANGELOG.md
git commit -m "docs: record Gate 8 durable queue terminal disposition delivery"
```

---

## Self-Review Notes

- **Spec coverage:** `WorkItemDisposition` enum (T1), failed set + partition (T2), `completeActiveVerified`/`failActive` split (T3), queue-stores-disposition-only and preserved `completedWorkItemIds` semantics (T2/T3), schema-v1-in-place serialization + fail-closed boundary (T4 + T6 decision), durable persist-before-expose + recovery (T5), docs/decision sync (T6). Out-of-scope items are explicitly excluded in Global Constraints.
- **Type consistency:** `completeActiveVerified`, `failActive`, `failedWorkItemIds()`, `dispositionOf` are used identically across in-memory (T3), durable (T5), and their tests. `WorkItemDisposition.satisfiesDependencies()` matches T1. Constructor parameter order (failed set last) is consistent across `initial`, `snapshot`, and `decode`.
- **Placeholder scan:** every code and command step is concrete; no TBD/TODO.
