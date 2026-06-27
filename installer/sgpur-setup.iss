[Setup]
AppName=SGPUR - Urgencia Renal
AppVersion=1.0.0
AppPublisher=Secretaria de Saude
AppId={{B7A4C2D1-9E3F-4A5B-8C6D-0E1F2A3B4C5D}
DefaultDirName={autopf}\SGPUR
DefaultGroupName=SGPUR - Urgencia Renal
OutputDir=..\dist
OutputBaseFilename=SGPUR-Setup
Compression=lzma2/ultra64
SolidCompression=yes
DisableProgramGroupPage=no
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayName=SGPUR - Urgencia Renal
UninstallDisplayIcon={app}\SGPUR.exe
WizardStyle=modern
WizardSizePercent=100

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Tasks]
Name: "desktopicon"; Description: "Criar atalho na Area de Trabalho"; GroupDescription: "Atalhos:"; Flags: checkedonce

[Files]
Source: "..\dist\desktop\SGPUR\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\SGPUR - Urgencia Renal"; Filename: "{app}\SGPUR.exe"; Comment: "Sistema de Gestao de Processos de Urgencia Renal"
Name: "{group}\Desinstalar SGPUR"; Filename: "{uninstallexe}"
Name: "{autodesktop}\SGPUR - Urgencia Renal"; Filename: "{app}\SGPUR.exe"; Tasks: desktopicon; Comment: "Sistema de Gestao de Processos de Urgencia Renal"

[Run]
Filename: "{app}\SGPUR.exe"; Description: "Iniciar SGPUR agora"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "taskkill.exe"; Parameters: "/F /IM SGPUR.exe"; Flags: runhidden; RunOnceId: "KillSGPUR"
