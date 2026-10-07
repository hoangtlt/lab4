param(
    [switch]$WindowsAuth,
    [int]$SqlPort = 1433
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)

if ($WindowsAuth) {
    $env:DB_URL = "jdbc:sqlserver://localhost:$SqlPort;databaseName=OrchidDB;encrypt=true;trustServerCertificate=true;integratedSecurity=true"
    $env:DB_USERNAME = ''
    $env:DB_PASSWORD = ''
    if (-not (Test-Path -LiteralPath 'tools/native/mssql-jdbc_auth-12.8.1.x64.dll')) {
        throw 'Chay scripts/setup-windows.ps1 truoc de tai DLL Windows Authentication.'
    }
}

mvn '-DskipTests' package
if ($LASTEXITCODE -ne 0) { throw 'Maven build that bai.' }

$nativePath = Join-Path (Get-Location) 'tools/native'
java "-Djava.library.path=$nativePath" -jar 'target/lab4-orchid-api-0.0.1-SNAPSHOT.jar'
