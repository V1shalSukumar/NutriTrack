$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$portableRoot = Join-Path (Split-Path (Split-Path $projectRoot -Parent) -Parent) 'work'
$jdk = Get-ChildItem -LiteralPath $portableRoot -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
    throw 'Java JDK 17 or newer is required. Set JAVA_HOME to its installation folder.'
}
$javaExe = Join-Path $env:JAVA_HOME 'bin/java.exe'
$mavenCommand = Join-Path $portableRoot 'apache-maven-3.9.9/bin/mvn.cmd'
if (-not (Test-Path $mavenCommand)) {
    $mavenCommand = (Get-Command mvn.cmd -ErrorAction Stop).Source
}
$mavenArgs = @()
if (Test-Path (Join-Path $portableRoot 'm2')) {
    $mavenArgs += ('-Dmaven.repo.local=' + (Join-Path $portableRoot 'm2'))
}
Set-Location -LiteralPath $projectRoot
