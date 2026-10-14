param(
    [string]$SigningDirectory = "$env:USERPROFILE/.codex/signing/routinenest",
    [string]$BuildToolsDirectory = "$env:LOCALAPPDATA/Android/Sdk/build-tools/36.0.0",
    [string]$UnsignedApk
)
$ErrorActionPreference = 'Stop'
$routineProject = Split-Path -Parent $PSScriptRoot
$routineUnsigned = Join-Path $routineProject 'app/build/outputs/apk/release/app-release-unsigned.apk'
if ($UnsignedApk) { $routineUnsigned = (Resolve-Path -LiteralPath $UnsignedApk).Path }
$routineSigned = Join-Path $routineProject 'app/build/outputs/apk/release/app-release.apk'
$routineKey = Join-Path $SigningDirectory 'routinenest-release.jks'
$routinePasswordPath = Join-Path $SigningDirectory 'password.dpapi'
$routineSecure = (Get-Content -LiteralPath $routinePasswordPath -Raw).Trim() | ConvertTo-SecureString
$routinePointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($routineSecure)
try {
    $env:ROUTINENEST_SIGNING_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($routinePointer)
    & (Join-Path $BuildToolsDirectory 'apksigner.bat') sign --ks $routineKey --ks-key-alias routinenest-release --ks-pass env:ROUTINENEST_SIGNING_PASSWORD --key-pass env:ROUTINENEST_SIGNING_PASSWORD --out $routineSigned $routineUnsigned
    if ($LASTEXITCODE -ne 0) { throw 'APK signing failed.' }
    & (Join-Path $BuildToolsDirectory 'apksigner.bat') verify --verbose --print-certs $routineSigned
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    & (Join-Path $BuildToolsDirectory 'zipalign.exe') -c -P 16 4 $routineSigned
    if ($LASTEXITCODE -ne 0) { throw 'APK ZIP alignment verification failed.' }
    Get-FileHash -LiteralPath $routineSigned -Algorithm SHA256
} finally {
    Remove-Item Env:/ROUTINENEST_SIGNING_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($routinePointer)
}
