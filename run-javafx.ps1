. (Join-Path $PSScriptRoot 'runtime.ps1')
& $mavenCommand @mavenArgs -B -ntp compile dependency:copy-dependencies '-DoutputDirectory=target/lib'
if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
$fxDir = Join-Path $PSScriptRoot 'target/javafx-modules'
if (Test-Path $fxDir) { Remove-Item -LiteralPath $fxDir -Recurse -Force }
New-Item -ItemType Directory -Path $fxDir -Force | Out-Null
Get-ChildItem 'target/lib/javafx-*-win.jar' | Where-Object { $_.Name -match '^javafx-(base|controls|graphics)-' } | Copy-Item -Destination $fxDir -Force
& $javaExe '--module-path' $fxDir '--add-modules' 'javafx.controls' '-cp' 'target/classes;target/lib/*' 'com.nutritrack.desktop.ReviewApp'
