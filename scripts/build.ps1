<#
.SYNOPSIS
  The one build script, run by CI and by the developer: licenses, build, test, package, checksums, smoke test.
.PARAMETER Target
  windows | linux | android. Default: the host OS (windows or linux).
.PARAMETER SkipSmoke
  Do not smoke-test the package. (Android is never smoke-tested here: CI does it on an emulator
  with scripts/smoke-android.sh.)
.NOTES
  Compatible with Windows PowerShell 5.1 and PowerShell 7. Exits non-zero on any real failure.
  Coverage is reported elsewhere as a warning and never fails this script.
  The Gradle task names below were confirmed locally on Windows in M0 (Linux and Android: by CI); change them here only.
#>
[CmdletBinding()]
param(
    [ValidateSet('windows', 'linux', 'android')]
    [string]$Target,
    [switch]$SkipSmoke
)

$ErrorActionPreference = 'Continue'
. (Join-Path $PSScriptRoot 'Common.ps1')
. (Join-Path $PSScriptRoot 'Checksums.ps1')
. (Join-Path $PSScriptRoot 'Package.ps1')
. (Join-Path $PSScriptRoot 'Smoke.ps1')

$GradleTasks = @{
    LicenseCheck = @('checkLicense')
    desktop      = @{ Test = @('jvmTest', ':desktopApp:test'); Build = @(':desktopApp:createDistributable') }
    android      = @{ Test = @('testAndroidHostTest'); Build = @(':androidApp:assembleRelease') }
}

function Stop-Build {
    param([string]$Message)
    Write-Host "BUILD FAILED: $Message" -ForegroundColor Red
    exit 1
}

function Get-TaskSet {
    param([string]$Platform)
    if ($Platform -eq 'android') {
        return $GradleTasks.android
    }
    return $GradleTasks.desktop
}

function New-TargetPackage {
    param([string]$Platform, [string]$Version)
    switch ($Platform) {
        'windows' { return New-WindowsPackage $Version }
        'linux' { return New-LinuxPackage $Version }
        'android' { return New-AndroidPackage $Version }
    }
}

if (-not $Target) {
    $Target = Get-HostTarget
}
if ($Target -eq 'windows' -and -not (Test-IsWindowsHost)) {
    Write-Warning 'The Windows package is built on Windows only; skipping.'
    exit 0
}
if ($Target -eq 'linux' -and (Test-IsWindowsHost)) {
    Write-Warning 'The Linux package is built on Linux only; skipping.'
    exit 0
}

$version = Get-AppVersion
if (-not $version) {
    Stop-Build 'no version'
}
$tasks = Get-TaskSet $Target
Write-Host "GlyphDraft $version, target $Target"

Write-Step 'Prepare dist/'
New-EmptyDirectory $script:DistDir

Write-Step 'Check dependency licenses'
if (-not (Invoke-Gradle $GradleTasks.LicenseCheck)) {
    Stop-Build 'license check'
}

Write-Step 'Build and test (warnings are errors, configured in Gradle)'
if (-not (Invoke-Gradle ($tasks.Test + $tasks.Build))) {
    Stop-Build 'build or tests'
}

Write-Step 'Package'
$package = New-TargetPackage $Target $version
if (-not $package) {
    Stop-Build 'packaging'
}
Write-Host "Created $package"

Write-Step 'Checksums'
$sums = Write-Checksums $Target
if (-not (Test-Checksums $sums)) {
    Stop-Build 'checksum verification'
}
Get-Content -LiteralPath $sums

if ($Target -eq 'android' -or $SkipSmoke) {
    Write-Host 'Smoke test not run here.'
}
else {
    Write-Step 'Smoke test the package'
    if (-not (Test-DesktopSmoke $package)) {
        Stop-Build 'smoke test'
    }
    Write-Host 'Smoke test passed.'
}

Write-Host ''
Write-Host 'BUILD OK' -ForegroundColor Green
exit 0
