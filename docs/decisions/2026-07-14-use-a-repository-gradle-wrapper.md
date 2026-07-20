# 2026-07-14: Use A Repository Gradle Wrapper

Status: Accepted Decision

Decision:

Enhancer will store a Gradle Wrapper in the repository and use Java 17 as the build runtime. A global Gradle installation is not required for normal project builds.

Rationale:

The Wrapper makes local development and future CI reproducible while matching the existing Java 17 Gradle build. It also removes reliance on user-specific cached Gradle paths.

Consequences:

- Developers run `gradlew.bat` on Windows or `./gradlew` on Unix-like systems.
- Wrapper scripts, properties, and the wrapper JAR are version-controlled.
- Java 17 remains an external prerequisite and is not committed to the repository.
