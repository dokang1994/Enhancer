param(
    [switch] $SkipTests
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
$jdkRoot = Join-Path $toolsRoot 'jdk17-runtime'
$jdkArchive = Join-Path $toolsRoot 'microsoft-jdk-17-windows-x64.zip'
$java = Get-ChildItem -LiteralPath $jdkRoot -Recurse -Filter java.exe -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match '\\bin\\java\.exe$' } |
    Select-Object -First 1

if (-not $java) {
    New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null
    & curl.exe -L --fail --retry 3 --output $jdkArchive 'https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip'
    if ($LASTEXITCODE -ne 0) {
        throw "Java 17 download failed with exit code $LASTEXITCODE."
    }

    if (Test-Path $jdkRoot) {
        Remove-Item -LiteralPath $jdkRoot -Recurse -Force
    }
    Expand-Archive -LiteralPath $jdkArchive -DestinationPath $jdkRoot -Force
}

& (Join-Path $PSScriptRoot 'gradle.ps1') --version
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not $SkipTests) {
    & (Join-Path $PSScriptRoot 'gradle.ps1') --no-daemon test
    exit $LASTEXITCODE
}
