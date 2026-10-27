param([int]$Port = 8080, [switch]$Rebuild)
. (Join-Path $PSScriptRoot 'runtime.ps1')
if ($Rebuild -or -not (Test-Path 'target/classes/com/nutritrack/Launcher.class') -or -not (Test-Path 'target/lib')) {
    & $mavenCommand @mavenArgs -B -ntp test package dependency:copy-dependencies '-DoutputDirectory=target/lib'
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. See the errors above.' }
}
Write-Host "Starting NutriTrack at http://localhost:$Port (Ctrl+C to stop)"
& $javaExe "-Dport=$Port" '-cp' 'target/classes;target/lib/*' 'com.nutritrack.Launcher'
