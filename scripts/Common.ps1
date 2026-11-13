# Shared helpers for build.ps1. Compatible with Windows PowerShell 5.1 and PowerShell 7.
# Dot-source this file; it does not run anything by itself.

$script:RepoRoot = Split-Path -Parent $PSScriptRoot
$script:DistDir = Join-Path $script:RepoRoot 'dist'
$script:AppName = 'GlyphDraft'

function Test-IsWindowsHost {
    return [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT
}

function Get-HostTarget {
    if (Test-IsWindowsHost) {
        return 'windows'
    }
    return 'linux'
}

function Write-Step {
    param([string]$Message)
    Write-Host ''
    Write-Host "== $Message" -ForegroundColor Cyan
}

function Get-AppVersion {
    $file = Join-Path $script:RepoRoot 'gradle.properties'
    $line = Select-String -Path $file -Pattern '^appVersion=(.+)$' | Select-Object -First 1
    if ($null -eq $line) {
        Write-Error "appVersion not found in $file"
        return $null
    }
    return $line.Matches[0].Groups[1].Value.Trim()
}

function Invoke-Gradle {
    # Returns $true on success; the caller decides what a failure means.
    param([string[]]$GradleArguments)
    $wrapper = 'gradlew'
    if (Test-IsWindowsHost) {
        $wrapper = 'gradlew.bat'
    }
    $path = Join-Path $script:RepoRoot $wrapper
    Write-Host "gradle $($GradleArguments -join ' ')"
    & $path @GradleArguments '--console=plain'
    return ($LASTEXITCODE -eq 0)
}

function Invoke-Native {
    # Runs a native program and returns $true when its exit code is 0.
    param([string]$Program, [string[]]$ProgramArguments)
    & $Program @ProgramArguments
    return ($LASTEXITCODE -eq 0)
}

function Invoke-WithRetry {
    # File operations on Windows can fail while antivirus holds a lock; try a few times.
    param([scriptblock]$Action, [int]$Attempts = 3)
    for ($i = 1; $i -le $Attempts; $i++) {
        try {
            & $Action
            return $true
        }
        catch {
            Write-Warning "Attempt $i of $Attempts failed: $($_.Exception.Message)"
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function New-EmptyDirectory {
    param([string]$Path)
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Path $Path | Out-Null
}
