# Smoke test of a desktop package: extract it into an empty folder, start it, and check that it
# stays alive and writes a log file into the data folder. Dot-source after Common.ps1.

function Expand-Package {
    param([string]$Package, [string]$Destination)
    if ($Package.EndsWith('.zip')) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [System.IO.Compression.ZipFile]::ExtractToDirectory($Package, $Destination)
        return $true
    }
    return (Invoke-Native 'tar' @('-xzf', $Package, '-C', $Destination))
}

function Get-LauncherPath {
    param([string]$ExtractedRoot)
    if (Test-IsWindowsHost) {
        return Join-Path $ExtractedRoot "$($script:AppName)/$($script:AppName).exe"
    }
    return Join-Path $ExtractedRoot "$($script:AppName)/bin/$($script:AppName)"
}

function Start-VirtualDisplay {
    # GUI apps need a display on CI Linux. Returns the Xvfb process, or $null on Windows.
    if (Test-IsWindowsHost) {
        return $null
    }
    $xvfb = Start-Process -FilePath 'Xvfb' -ArgumentList @(':99', '-screen', '0', '1280x720x24') -PassThru
    $env:DISPLAY = ':99'
    Start-Sleep -Seconds 2
    return $xvfb
}

function Wait-ForLogFile {
    param([string]$LogDirectory, [System.Diagnostics.Process]$App, [int]$TimeoutSeconds = 40)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($App.HasExited) {
            Write-Warning "App exited early with code $($App.ExitCode)"
            return $false
        }
        $logs = Get-ChildItem -LiteralPath $LogDirectory -Filter '*.log' -ErrorAction SilentlyContinue
        if ($null -ne $logs) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    Write-Warning "No *.log file in $LogDirectory after $TimeoutSeconds s"
    return $false
}

function Test-DesktopSmoke {
    param([string]$Package)
    $work = Join-Path ([System.IO.Path]::GetTempPath()) "glyphdraft-smoke-$([guid]::NewGuid().ToString('N'))"
    New-EmptyDirectory $work
    $app = $null
    $display = $null
    $passed = $false
    try {
        if (-not (Expand-Package $Package $work)) {
            Write-Warning 'Could not extract the package'
            return $false
        }
        $dataDir = Join-Path $work 'data'
        $env:GLYPHDRAFT_DATA_DIR = $dataDir
        $display = Start-VirtualDisplay
        $app = Start-Process -FilePath (Get-LauncherPath $work) -PassThru
        $passed = Wait-ForLogFile (Join-Path $dataDir 'logs') $app
        Start-Sleep -Seconds 3
        if ($passed -and $app.HasExited) {
            Write-Warning "App exited after starting, code $($app.ExitCode)"
            $passed = $false
        }
    }
    finally {
        if ($null -ne $app -and -not $app.HasExited) {
            Stop-Process -Id $app.Id -Force
        }
        if ($null -ne $display -and -not $display.HasExited) {
            Stop-Process -Id $display.Id -Force
        }
        Remove-Item Env:\GLYPHDRAFT_DATA_DIR -ErrorAction SilentlyContinue
        Invoke-WithRetry { Remove-Item -LiteralPath $work -Recurse -Force } | Out-Null
    }
    return $passed
}
