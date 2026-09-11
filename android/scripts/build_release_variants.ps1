[CmdletBinding()]
param(
    [string[]] $Abis = @("arm64-v8a", "armeabi-v7a", "x86_64"),
    [string] $OutputDirectory = ""
)

$ErrorActionPreference = "Stop"
$supportedAbis = @("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
$androidRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $androidRoot "gradlew.bat"
$keystoreProperties = Join-Path $androidRoot "keystore.properties"

if (-not (Test-Path -LiteralPath $keystoreProperties)) {
    throw "keystore.properties is required to create installable release APKs."
}

foreach ($abi in $Abis) {
    if ($abi -notin $supportedAbis) {
        throw "Unsupported ABI '$abi'. Supported values: $($supportedAbis -join ', ')."
    }
}

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $androidRoot "app\build\outputs\apk\release\variants"
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
    $env:GRADLE_USER_HOME = Join-Path ([System.IO.Path]::GetTempPath()) "scoop-gradle"
}

Push-Location $androidRoot
try {
    foreach ($abi in $Abis) {
        Write-Host "Building signed release APK for $abi..."
        & $gradleWrapper --no-daemon :app:assembleRelease "-PscoopAbi=$abi"
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle failed while building $abi."
        }

        $sourceApk = Join-Path $androidRoot "app\build\outputs\apk\release\app-release.apk"
        if (-not (Test-Path -LiteralPath $sourceApk)) {
            throw "Expected signed APK was not produced: $sourceApk"
        }

        $destination = Join-Path $OutputDirectory "Scoop-$abi.apk"
        Copy-Item -LiteralPath $sourceApk -Destination $destination -Force
        $sizeMb = [math]::Round((Get-Item -LiteralPath $destination).Length / 1MB, 2)
        Write-Host "Created $destination ($sizeMb MiB)"
    }
}
finally {
    Pop-Location
}
