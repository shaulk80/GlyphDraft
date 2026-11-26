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

# Failures that say the dependency server, not our code, was the problem (Maven Central answers 429 to busy CI runners).
# For a plugin that cannot be downloaded Gradle prints only "could not resolve plugin artifact", not the server's answer.
$script:TransientGradleFailure = 'Received status code 429|Too Many Requests|Read timed out|Connect timed out|Connection reset|could not resolve plugin artifact'
$script:GradleRetryDelaySeconds = 30

function ConvertTo-PlainLine {
    # A line of program output as text: stderr lines arrive as error records whose message is the line.
    param($Line)
    if ($Line -is [System.Management.Automation.ErrorRecord]) {
        return $Line.Exception.Message
    }
    return "$Line"
}

function Invoke-GradleOnce {
    # Out-Host: the program's output must not become part of the return value (an array is always "true").
    # Tee-Object keeps a copy of the output in $LogFile so the caller can look at why it failed. Gradle reports
    # errors on stderr, so that is merged in; ConvertTo-PlainLine turns each line into plain text (Windows PowerShell
    # 5.1 would otherwise print stderr lines as red error records).
    param([string[]]$GradleArguments, [string]$LogFile)
    Write-Host "gradle $($GradleArguments -join ' ')"
    if (Test-IsWindowsHost) {
        & (Join-Path $script:RepoRoot 'gradlew.bat') @GradleArguments '--console=plain' 2>&1 | ForEach-Object { ConvertTo-PlainLine $_ } | Tee-Object -FilePath $LogFile | Out-Host
    }
    else {
        # Run through sh so a missing executable bit on gradlew (files made on Windows) does not matter.
        & sh (Join-Path $script:RepoRoot 'gradlew') @GradleArguments '--console=plain' 2>&1 | ForEach-Object { ConvertTo-PlainLine $_ } | Tee-Object -FilePath $LogFile | Out-Host
    }
    return ($LASTEXITCODE -eq 0)
}

function Invoke-Gradle {
    # Returns $true on success; the caller decides what a failure means.
    # A run that failed because a dependency or plugin download failed is repeated once (logged); any other failure,
    # a failing test for example, is final at once.
    param([string[]]$GradleArguments)
    $log = [System.IO.Path]::GetTempFileName()
    try {
        if (Invoke-GradleOnce $GradleArguments $log) {
            return $true
        }

        if (-not (Select-String -Path $log -Pattern $script:TransientGradleFailure -Quiet)) {
            return $false
        }

        Write-Host "RETRY: gradle failed on a dependency or plugin download; waiting $($script:GradleRetryDelaySeconds) s and trying once more." -ForegroundColor Yellow
        Start-Sleep -Seconds $script:GradleRetryDelaySeconds
        return (Invoke-GradleOnce $GradleArguments $log)
    }
    finally {
        Remove-Item -LiteralPath $log -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-Native {
    # Runs a native program and returns $true when its exit code is 0.
    param([string]$Program, [string[]]$ProgramArguments)
    & $Program @ProgramArguments | Out-Host
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
