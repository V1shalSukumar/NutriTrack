. (Join-Path $PSScriptRoot 'runtime.ps1')
& $mavenCommand @mavenArgs -B -ntp test
if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
