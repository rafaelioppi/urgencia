# release.ps1 — Atualiza o main e regera o executavel + instalador do SGPUR.
#
# Uso:
#   .\release.ps1           -> git pull + rebuild + installer (padrao)
#   .\release.ps1 -SemPull  -> skip git pull (so rebuild)
#   .\release.ps1 -SomenteInstaller -> so roda o Inno Setup (exe ja pronto)

param(
    [switch]$SemPull,
    [switch]$SomenteInstaller
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

function Titulo($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Ok($msg)     { Write-Host "    $msg" -ForegroundColor Green }
function Erro($msg)   { Write-Host "ERRO: $msg" -ForegroundColor Red; exit 1 }

# --- 1. Git pull no main --------------------------------------------------
if (-not $SemPull -and -not $SomenteInstaller) {
    Titulo "Verificando branch..."
    $branch = git -C $root rev-parse --abbrev-ref HEAD
    if ($branch -ne "main") {
        Write-Host "    Branch atual: $branch (nao e main). Mudando para main..." -ForegroundColor Yellow
        git -C $root checkout main
    }

    $antes = git -C $root rev-parse HEAD
    Titulo "git pull origin main..."
    git -C $root pull origin main
    $depois = git -C $root rev-parse HEAD

    if ($antes -eq $depois) {
        Write-Host "    Nenhuma mudanca no main. Rebuild mesmo assim? (S/N)" -NoNewline
        $r = Read-Host " "
        if ($r -notmatch '^[Ss]') { Write-Host "Cancelado."; exit 0 }
    } else {
        Ok "Atualizado: $($antes.Substring(0,7)) -> $($depois.Substring(0,7))"
    }
}

# --- 2. Build exe (JAR + jpackage) ----------------------------------------
if (-not $SomenteInstaller) {
    Titulo "Gerando SGPUR.exe (package-desktop.ps1)..."
    & "$root\package-desktop.ps1"
    if ($LASTEXITCODE -ne 0) { Erro "package-desktop.ps1 falhou (exit $LASTEXITCODE)." }
    Ok "Executavel: dist\desktop\SGPUR\SGPUR.exe"
}

# --- 3. Installer (Inno Setup) ---------------------------------------------
$iscc = "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
if (-not (Test-Path $iscc)) { Erro "Inno Setup nao encontrado em '$iscc'. Instale em https://jrsoftware.org/isinfo.php" }

$iss = "$root\installer\sgpur-setup.iss"
if (-not (Test-Path $iss)) { Erro "Arquivo .iss nao encontrado: $iss" }

Titulo "Gerando SGPUR-Setup.exe (Inno Setup)..."
& $iscc $iss
if ($LASTEXITCODE -ne 0) { Erro "Inno Setup falhou (exit $LASTEXITCODE)." }

$setup = "$root\dist\SGPUR-Setup.exe"
if (Test-Path $setup) {
    $tamanho = "{0:N1} MB" -f ((Get-Item $setup).Length / 1MB)
    Ok "Instalador: dist\SGPUR-Setup.exe ($tamanho)"
} else {
    Erro "SGPUR-Setup.exe nao foi gerado."
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  Release pronto!" -ForegroundColor Green
Write-Host "  Executavel : dist\desktop\SGPUR\SGPUR.exe"
Write-Host "  Instalador : dist\SGPUR-Setup.exe"
Write-Host "  ZIP        : dist\SGPUR-desktop.zip"
Write-Host "========================================`n" -ForegroundColor Green
