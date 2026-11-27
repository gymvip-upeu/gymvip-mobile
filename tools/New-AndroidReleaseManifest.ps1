[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $ApkPath,
    [Parameter(Mandatory)] [string] $AaptPath,
    [Parameter(Mandatory)] [string] $ApkSignerPath,
    [Parameter(Mandatory)] [string] $ExpectedSigningSha256,
    [Parameter(Mandatory)] [uri] $DownloadUrl
)

$ErrorActionPreference = 'Stop'
$apk = Get-Item -LiteralPath $ApkPath
$expectedFingerprint = ($ExpectedSigningSha256 -replace ':', '').ToLowerInvariant()
if ($apk.PSIsContainer -or $apk.Extension -ne '.apk' -or $apk.Length -le 0) {
    throw 'Indica un APK firmado y no vacío.'
}
if ($expectedFingerprint -notmatch '^[0-9a-f]{64}$') {
    throw 'El SHA-256 esperado debe corresponder al certificado oficial de firma.'
}
if (!$DownloadUrl.IsAbsoluteUri -or $DownloadUrl.Scheme -ne 'https' -or $DownloadUrl.UserInfo) {
    throw 'Indica una URL HTTPS pública sin credenciales.'
}

$signature = (& $ApkSignerPath verify --print-certs $apk.FullName | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'La firma del APK no es válida.' }
$certificateDigests = [regex]::Matches($signature, '(?im)^Signer #\d+ certificate SHA-256 digest:\s*([0-9a-f]{64})\s*$')
if ($certificateDigests.Count -ne 1 -or $certificateDigests[0].Groups[1].Value -ine $expectedFingerprint) {
    throw 'La firma no corresponde al certificado oficial configurado.'
}

$badging = (& $AaptPath dump badging $apk.FullName | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'No se pudo leer la identidad y versión del APK.' }
$package = [regex]::Match($badging, "(?m)^package:\s+name='(?<id>[^']+)'\s+versionCode='(?<code>\d+)'\s+versionName='(?<version>[^']+)'")
if (!$package.Success -or $package.Groups['id'].Value -ne 'com.gymvip.mobile') {
    throw 'El APK no corresponde a com.gymvip.mobile.'
}
$hash = (Get-FileHash -LiteralPath $apk.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
[ordered]@{
    platform = 'ANDROID'
    channel = 'PRODUCTION'
    versionCode = [int] $package.Groups['code'].Value
    versionName = $package.Groups['version'].Value
    fileName = $apk.Name
    fileSizeBytes = $apk.Length
    sha256Checksum = $hash
    downloadUrl = $DownloadUrl.AbsoluteUri
} | ConvertTo-Json
