# Empacota o SGPUR como aplicativo desktop (Windows) usando jpackage.
# Gera uma pasta com SGPUR.exe + Java embutido (o usuario NAO precisa ter Java).
# Saida: dist\desktop\SGPUR\SGPUR.exe  e  dist\SGPUR-desktop.zip
#
# Uso:  .\package-desktop.ps1

$jdk21 = "C:\Users\rafae\Tools\jdk-21.0.11+10"
if (-not (Test-Path "$jdk21\bin\jpackage.exe")) {
    Write-Host "jpackage do JDK 21 nao encontrado em $jdk21\bin." -ForegroundColor Red
    exit 1
}
$env:JAVA_HOME = $jdk21
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

$mvn = "C:\Users\rafae\Tools\apache-maven-3.9.6\bin\mvn.cmd"
if (-not (Test-Path $mvn)) { $mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source }
if (-not $mvn) { Write-Host "Maven nao encontrado." -ForegroundColor Red; exit 1 }

$root = $PSScriptRoot
Write-Host "==> Build do JAR..." -ForegroundColor Cyan
& $mvn -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { Write-Host "Build falhou." -ForegroundColor Red; exit 1 }

$jar = Get-ChildItem "$root\target\*.jar" | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1
if (-not $jar) { Write-Host "JAR nao encontrado em target\." -ForegroundColor Red; exit 1 }

# diretorio de entrada contendo apenas o jar
$input = "$root\target\jpackage-input"
New-Item -ItemType Directory -Path $input -Force | Out-Null
Copy-Item $jar.FullName "$input\sgpur.jar" -Force

$destDir = "$root\dist\desktop"
New-Item -ItemType Directory -Path $destDir -Force | Out-Null

# remove app-image anterior, se houver (jpackage falha se o destino ja existir)
if (Test-Path "$destDir\SGPUR") { Remove-Item "$destDir\SGPUR" -Recurse -Force -Confirm:$false }

$jpArgs = @(
    "--type", "app-image",
    "--name", "SGPUR",
    "--input", $input,
    "--main-jar", "sgpur.jar",
    "--java-options", "-Dspring.profiles.active=desktop",
    "--java-options", "-Xmx512m",
    "--app-version", "1.0.0",
    "--vendor", "Secretaria de Saude",
    "--dest", $destDir
)
$ico = "$root\deploy\sgpur.ico"
if (Test-Path $ico) { $jpArgs += @("--icon", $ico) }

Write-Host "==> jpackage (pode levar 1-2 min)..." -ForegroundColor Cyan
& "$env:JAVA_HOME\bin\jpackage.exe" @jpArgs
if ($LASTEXITCODE -ne 0) { Write-Host "jpackage falhou." -ForegroundColor Red; exit 1 }

$zip = "$root\dist\SGPUR-desktop.zip"
Compress-Archive -Path "$destDir\SGPUR\*" -DestinationPath $zip -Force
Write-Host "==> Pronto!" -ForegroundColor Green
Write-Host "Executavel: $destDir\SGPUR\SGPUR.exe"
Write-Host "ZIP:        $zip"
