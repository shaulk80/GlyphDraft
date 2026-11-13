# SHA256 checksum files in the format of `sha256sum` ("<hash>  <name>", LF line ends, no BOM).
# Dot-source after Common.ps1.

function Write-Checksums {
    # Writes dist/SHA256SUMS-<Target>.txt for every package in dist, sorted by file name.
    param([string]$Target)
    $files = Get-ChildItem -LiteralPath $script:DistDir -File |
        Where-Object { $_.Name -notlike 'SHA256SUMS*' } |
        Sort-Object { $_.Name } -CaseSensitive
    $lines = foreach ($file in $files) {
        $hash = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $($file.Name)"
    }
    $text = ($lines -join "`n") + "`n"
    $path = Join-Path $script:DistDir "SHA256SUMS-$Target.txt"
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, $text, $encoding)
    return $path
}

function Test-Checksums {
    # Verifies a checksum file against the files next to it. Returns $true when all match.
    param([string]$ChecksumFile)
    $directory = Split-Path -Parent $ChecksumFile
    $allMatch = $true
    foreach ($line in Get-Content -LiteralPath $ChecksumFile) {
        if ($line -notmatch '^([0-9a-f]{64})  (.+)$') {
            Write-Warning "Bad checksum line: $line"
            $allMatch = $false
            continue
        }
        $expected = $Matches[1]
        $file = Join-Path $directory $Matches[2]
        $actual = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($actual -ne $expected) {
            Write-Warning "Checksum mismatch: $file"
            $allMatch = $false
        }
    }
    return $allMatch
}
