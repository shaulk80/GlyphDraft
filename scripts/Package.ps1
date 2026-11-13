# Packaging per target. Each function returns the path of the package it created, or $null on failure.
# Windows zips are made on Windows, Linux tar.gz on Linux (a zip made on Windows loses the executable bit).
# Dot-source after Common.ps1.

function Get-DesktopAppImage {
    # The Compose Desktop `createDistributable` task writes the app image here.
    $root = Join-Path $script:RepoRoot 'desktopApp/build/compose/binaries/main/app'
    $image = Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $image) {
        Write-Error "No app image under $root. Check the createDistributable task."
        return $null
    }
    return $image.FullName
}

function New-WindowsPackage {
    param([string]$Version)
    $image = Get-DesktopAppImage
    if ($null -eq $image) {
        return $null
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = Join-Path $script:DistDir "$($script:AppName)-$Version-windows-x64.zip"
    $created = Invoke-WithRetry {
        if (Test-Path -LiteralPath $zip) {
            Remove-Item -LiteralPath $zip -Force
        }
        [System.IO.Compression.ZipFile]::CreateFromDirectory($image, $zip, 'Optimal', $true)
    }
    if (-not $created) {
        Write-Error "Could not create $zip"
        return $null
    }
    return $zip
}

function New-LinuxPackage {
    param([string]$Version)
    if (Test-IsWindowsHost) {
        Write-Warning 'Linux packages are built on Linux only; skipping.'
        return $null
    }
    $image = Get-DesktopAppImage
    if ($null -eq $image) {
        return $null
    }
    # Directories 755, files 644, files that were executable 755.
    if (-not (Invoke-Native 'chmod' @('-R', 'u=rwX,go=rX', $image))) {
        Write-Error 'chmod failed'
        return $null
    }
    $tar = Join-Path $script:DistDir "$($script:AppName)-$Version-linux-x64.tar.gz"
    $parent = Split-Path -Parent $image
    $name = Split-Path -Leaf $image
    $tarArgs = @('--owner=0', '--group=0', '--numeric-owner', '-czf', $tar, '-C', $parent, $name)
    if (-not (Invoke-Native 'tar' $tarArgs)) {
        Write-Error 'tar failed'
        return $null
    }
    return $tar
}

function New-AndroidPackage {
    param([string]$Version)
    $apkDir = Join-Path $script:RepoRoot 'androidApp/build/outputs/apk/release'
    $apk = Get-ChildItem -LiteralPath $apkDir -Filter '*.apk' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $apk) {
        Write-Error "No APK under $apkDir. Check the assembleRelease task and the signing config."
        return $null
    }
    $target = Join-Path $script:DistDir "$($script:AppName)-$Version-android.apk"
    Copy-Item -LiteralPath $apk.FullName -Destination $target -Force
    return $target
}
