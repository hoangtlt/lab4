param([string]$SqlInstance = '.\SQL2022')

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)

# Tải DLL chính thức để JDBC dùng tài khoản Windows hiện tại.
$nativePath = Join-Path (Get-Location) 'tools/native'
New-Item -ItemType Directory -Path $nativePath -Force | Out-Null
$dllPath = Join-Path $nativePath 'mssql-jdbc_auth-12.8.1.x64.dll'
if (-not (Test-Path -LiteralPath $dllPath)) {
    $zipPath = Join-Path $nativePath 'mssql-jdbc_auth.zip'
    Invoke-WebRequest -Uri 'https://github.com/microsoft/mssql-jdbc/releases/download/v12.8.1/mssql-jdbc_auth.zip' -OutFile $zipPath
    Expand-Archive -LiteralPath $zipPath -DestinationPath (Join-Path $nativePath 'download') -Force
    $downloadedDll = Get-ChildItem -LiteralPath (Join-Path $nativePath 'download') -Recurse -Filter 'mssql-jdbc_auth-12.8.1.x64.dll' | Select-Object -First 1
    if ($null -eq $downloadedDll) { throw 'Khong tim thay DLL x64 trong goi Microsoft.' }
    Copy-Item -LiteralPath $downloadedDll.FullName -Destination $dllPath
}

sqlcmd -S $SqlInstance -E -C -b -i 'sql/01-create-database.sql'
if ($LASTEXITCODE -ne 0) { throw 'Khong tao duoc OrchidDB. Kiem tra SQL instance va quyen Windows.' }
Write-Host 'Da tao OrchidDB va tai DLL. Chay scripts/run-local.ps1 -WindowsAuth.'
