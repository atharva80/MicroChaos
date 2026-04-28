param(
    [switch]$SkipDocker
)

$ErrorActionPreference = "Stop"

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message" -ForegroundColor Cyan
}

function Test-CommandExists($commandName) {
    return $null -ne (Get-Command $commandName -ErrorAction SilentlyContinue)
}

function Install-WingetPackage($id, $label) {
    Write-Step "Checking $label"

    $alreadyInstalled = winget list --exact --id $id 2>$null | Out-String
    if ($alreadyInstalled -match [regex]::Escape($id)) {
        Write-Host "$label is already installed." -ForegroundColor Green
        return
    }

    Write-Host "Installing $label..." -ForegroundColor Yellow
    winget install --exact --id $id --accept-source-agreements --accept-package-agreements
}

Write-Step "Validating Windows prerequisites"

if ($env:OS -ne "Windows_NT") {
    throw "Run this script from Windows PowerShell, not from WSL."
}

if (-not (Test-CommandExists "winget")) {
    throw "winget is required but was not found. Install App Installer from Microsoft Store first."
}

Install-WingetPackage "Microsoft.OpenJDK.21" "OpenJDK 21"
Install-WingetPackage "Apache.Maven" "Apache Maven"

if (-not $SkipDocker) {
    Install-WingetPackage "Docker.DockerDesktop" "Docker Desktop"
}

Write-Step "Checking WSL availability"
if (-not (Test-CommandExists "wsl.exe")) {
    Write-Warning "wsl.exe was not found. Install/enable WSL from an elevated PowerShell prompt with: wsl --install"
} else {
    & wsl.exe --status
}

Write-Step "Setup summary"
Write-Host "Windows-side setup completed." -ForegroundColor Green
Write-Host "Next steps:" -ForegroundColor Green
Write-Host "1. Start Docker Desktop"
Write-Host "2. Open WSL"
Write-Host "3. Run scripts/setup-wsl.sh from inside WSL"
