param([string]$DeviceSerial)
$ErrorActionPreference = 'Stop'
$mobileRepoPath = Split-Path -Parent $PSScriptRoot
$apkPath = Join-Path $mobileRepoPath 'composeApp/build/outputs/apk/debug/composeApp-debug.apk'
$adbCommand = Get-Command adb -ErrorAction Stop
$deviceRows = & $adbCommand.Source devices
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar ADB.' }
$phoneSerials = @($deviceRows | Where-Object { $_ -match '^\S+\s+device\s*$' -and $_ -notmatch '^emulator-' } | ForEach-Object { ($_ -split '\s+')[0] })
if (!$DeviceSerial) {
    if ($phoneSerials.Count -ne 1) { throw 'Conecta y autoriza un solo teléfono por USB, o indica -DeviceSerial.' }
    $DeviceSerial = $phoneSerials[0]
}
if ($DeviceSerial -notin $phoneSerials) { throw 'El teléfono solicitado no aparece conectado y autorizado.' }
foreach ($serviceUrl in @('http://127.0.0.1:8081/api/v1/health','http://127.0.0.1:5173/restablecer-contrasena')) {
    $response = Invoke-WebRequest $serviceUrl -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -ne 200) { throw 'Verifica que el backend y la web estén encendidos.' }
}
if (!(Test-Path -LiteralPath $apkPath)) { throw 'No se encontró el APK para Android físico.' }
foreach ($servicePort in @(8081,5173)) {
    & $adbCommand.Source -s $DeviceSerial reverse "tcp:$servicePort" "tcp:$servicePort"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo conectar el puerto $servicePort por USB." }
}
& $adbCommand.Source -s $DeviceSerial install -r $apkPath
if ($LASTEXITCODE -ne 0) { throw 'La instalación falló. No se borraron datos ni se desinstaló otra versión.' }
& $adbCommand.Source -s $DeviceSerial shell am start -n com.gymvip.mobile.local/com.gymvip.mobile.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir la app.' }
'App de prueba instalada. Backend y enlaces de correo conectados por USB. Mantén el cable conectado y los servicios encendidos.'
'Prueba login, Inicio, membresía sin plan, FAQ, Avisos, recuperación, cierre de sesiones y reingreso. Push real se valida aparte cuando Firebase esté configurado.'
'Para retirar únicamente estas conexiones: adb -s <serial> reverse --remove tcp:8081 y adb -s <serial> reverse --remove tcp:5173.'
