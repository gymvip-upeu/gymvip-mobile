# Herramientas nativas Android para QA local. No se incorporan al APK.
$script:QaRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$script:QaCaptureDir = Join-Path $script:QaRoot 'qa/phase5/capturas'
$sdkDir = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else {
    Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Android/Sdk'
}
$script:QaAdb = Join-Path $sdkDir 'platform-tools/adb.exe'
if (!(Test-Path -LiteralPath $script:QaAdb)) { throw 'Configura ANDROID_HOME antes de ejecutar QA Android.' }
function Get-AndroidUi {
    $message = & $script:QaAdb -s emulator-5554 shell uiautomator dump /sdcard/Download/gymvip-ui.xml
    if ($message -notmatch 'dumped to') { throw 'Android no devolvió un árbol actual; vuelve a observar antes de actuar.' }
    & $script:QaAdb -s emulator-5554 pull /sdcard/Download/gymvip-ui.xml (Join-Path $script:QaCaptureDir 'ui.xml') 2>&1 | Out-Null
    return [xml]([IO.File]::ReadAllText((Join-Path $script:QaCaptureDir 'ui.xml')))
}
function Tap-AndroidNode($Node) {
    $coordinates = [regex]::Matches($Node.bounds, '\d+') | ForEach-Object { [int]$_.Value }
    if ($coordinates.Count -ne 4) { throw 'El control observado no tiene límites válidos.' }
    & $script:QaAdb -s emulator-5554 shell input tap ([int](($coordinates[0]+$coordinates[2])/2)) ([int](($coordinates[1]+$coordinates[3])/2))
}
function Tap-AndroidText([string]$Text) {
    $ui = Get-AndroidUi
    $nodes = @($ui.SelectNodes('//node') | Where-Object { $_.text -eq $Text -and $_.bounds -ne '[0,0][0,0]' })
    if ($nodes.Count -gt 1) { $nodes = @($nodes | Where-Object { $_.clickable -eq 'true' -or $_.ParentNode.clickable -eq 'true' }) }
    if ($nodes.Count -ne 1) { throw "Se esperaba un control único para '$Text'; encontrados: $($nodes.Count)." }
    Tap-AndroidNode $nodes[0]
}
function Set-AndroidField([int]$Index, [string]$Value) {
    # input text se ejecuta en Android; limitar caracteres impide interpretar comandos remotos.
    if ($Value -match '[^A-Za-z0-9@._!%+\- ]') { throw 'Usa texto ASCII seguro para la entrada por ADB.' }
    $ui = Get-AndroidUi
    $fields = @($ui.SelectNodes('//node[@class="android.widget.EditText"]'))
    if ($Index -lt 0 -or $Index -ge $fields.Count) { throw 'Campo Android inexistente.' }
    Tap-AndroidNode $fields[$Index]
    & $script:QaAdb -s emulator-5554 shell input keycombination 113 29
    & $script:QaAdb -s emulator-5554 shell input text ($Value.Replace(' ', '%s'))
    & $script:QaAdb -s emulator-5554 shell input keyevent 4
}
function Save-AndroidScreenshot([string]$Name) {
    if ($Name -notmatch '^[a-z0-9-]+$') { throw 'Nombre de captura inválido.' }
    & $script:QaAdb -s emulator-5554 shell screencap -p /sdcard/Download/gymvip-phase5.png
    & $script:QaAdb -s emulator-5554 pull /sdcard/Download/gymvip-phase5.png (Join-Path $script:QaCaptureDir "$Name.png") 2>&1 | Out-Null
}
function Tap-AndroidTab([string]$Text) {
    $ui = Get-AndroidUi
    $nodes = @($ui.SelectNodes('//node') | Where-Object { $_.text -eq $Text } |
        Sort-Object { [int]([regex]::Matches($_.bounds, '\d+')[3].Value) } -Descending)
    if ($nodes.Count -lt 1) { throw "Pestaña no visible: $Text" }
    Tap-AndroidNode $nodes[0]
}
function Wait-AndroidText([string]$Text) {
    for ($attempt = 0; $attempt -lt 8; $attempt++) {
        try { $ui = Get-AndroidUi } catch { Start-Sleep -Seconds 1; continue }
        if (@($ui.SelectNodes('//node') | Where-Object { $_.text -eq $Text }).Count -gt 0) { return }
        Start-Sleep -Seconds 1
    }
    throw "No apareció el control esperado: $Text"
}
function Show-AndroidTexts([int]$Limit = 20) {
    $ui = Get-AndroidUi
    $ui.SelectNodes('//node[@text!="" and @password!="true"]') | Select-Object -First $Limit text,bounds | Format-Table -AutoSize
}
