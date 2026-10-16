# -Neo: тот же прогон поверх NeoForge 1.20.1 (gradle -Pneoforge)
param([string]$Only = "", [switch]$Neo)
Set-Location D:\VScode\dev\thaumcraft4-1.20
# Если человек сам играет из этой же папки, самосъёмка не запускается: она стёрла бы его журнал и снимки.
$others = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like "*forgeclientuserdev*" -and $_.CommandLine -notlike "*thaumcraft.autoshot*" }
if ($others) { "busy=игра уже запущена человеком, самосъёмка отменена"; exit 0 }
$log = "run\logs\latest.log"
# Журнал последнего сеанса самого игрока не стираем, а откладываем: по нему ищут причину вылета.
if (Test-Path $log) {
  if (-not (Select-String -Path $log -Pattern "AUTOSHOT" -Quiet)) { Copy-Item $log "run\logs\last-player-session.log" -Force }
  Remove-Item $log -Force
}
if (Test-Path "run\saves\autoshot") { Remove-Item "run\saves\autoshot" -Recurse -Force }
if (Test-Path "run\screenshots") { Remove-Item "run\screenshots\*.png" -Force }
$gradleArgs = @("runClient", "--offline", $(if ($Only) { "-Pautoshot=$Only" } else { "-Pautoshot" }), "-q")
if ($Neo) { $gradleArgs += "-Pneoforge" }
$p = Start-Process -FilePath ".\gradlew.bat" -ArgumentList $gradleArgs -PassThru -WindowStyle Hidden -RedirectStandardOutput "build\client.out" -RedirectStandardError "build\client.err"
$done = $false
for ($i = 0; $i -lt 1380; $i++) {
  Start-Sleep -Seconds 1
  if ($p.HasExited) { $done = $true; break }
  if (Test-Path $log) {
    $t = Get-Content $log -Raw -ErrorAction SilentlyContinue
    if ($t -match "AUTOSHOT готово" -or $t -match "AUTOSHOT сбой") { Start-Sleep -Seconds 2; $done = $true; break }
  }
}
"done=$done"
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like "*thaumcraft4-1.20*" -and $_.CommandLine -like "*thaumcraft.autoshot*" } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
if (Test-Path $log) { Select-String -Path $log -Pattern "AUTOSHOT сбой|AUTOSHOT CHECK|thaumcraft.*(ERROR|Exception)|ERROR.*thaumcraft|Caused by|^\tat thaumcraft" -Encoding UTF8 | Select-Object -First 25 | ForEach-Object { $_.Line.Substring(0, [Math]::Min(260, $_.Line.Length)) } }
(Get-ChildItem "run\screenshots\*.png" -ErrorAction SilentlyContinue | Measure-Object).Count
