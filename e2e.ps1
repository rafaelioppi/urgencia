# Roda o teste E2E de navegador (Playwright) do SAUR (Windows / PowerShell),
# forcando o JDK 21. Sobe o app real (H2, porta aleatoria) e abre um Chromium
# de verdade que percorre todo o fluxo do processo clicando na tela.
# Uso:  .\e2e.ps1                 -> roda o(s) teste(s) E2E (headless)
#       .\e2e.ps1 -Headed         -> roda com o browser visivel (mais lento, p/ depurar)
#       .\e2e.ps1 -InstalarBrowser -> so instala o Chromium do Playwright (1a vez)

param(
    [switch]$Headed,
    [switch]$InstalarBrowser
)

# --- Java 21 (forca o JDK 21, mesmo que JAVA_HOME aponte para outra versao) ---
$jdk21 = "C:\Users\rafae\Tools\jdk-21.0.11+10"
if (Test-Path "$jdk21\bin\java.exe") {
    $env:JAVA_HOME = $jdk21
} elseif (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Host "JDK 21 nao encontrado. Instale o Temurin 21 ou defina JAVA_HOME para um JDK 21." -ForegroundColor Red
    exit 1
}
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

# --- Maven (PATH ou caminho conhecido) ---
$mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
    $cand = "C:\Users\rafae\Tools\apache-maven-3.9.6\bin\mvn.cmd"
    if (Test-Path $cand) { $mvn = $cand }
}
if (-not $mvn) {
    Write-Host "Maven nao encontrado. Instale o Maven ou ajuste o caminho em e2e.ps1." -ForegroundColor Red
    exit 1
}

if ($InstalarBrowser) {
    Write-Host "==> Instalando o Chromium do Playwright..." -ForegroundColor Cyan
    & $mvn -q dependency:build-classpath "-Dmdep.outputFile=target/e2e-classpath.txt" -DincludeScope=test
    $cp = Get-Content "target/e2e-classpath.txt" -Raw
    & "$env:JAVA_HOME\bin\java.exe" -cp "$cp;target\test-classes;target\classes" `
        com.microsoft.playwright.CLI install chromium
    exit $LASTEXITCODE
}

if ($Headed) { $env:SAUR_E2E_HEADED = "true" }

Write-Host "==> Rodando teste E2E (Playwright) | JAVA_HOME: $env:JAVA_HOME | Headed: $($Headed.IsPresent)" -ForegroundColor Cyan
& $mvn verify -Pe2e
