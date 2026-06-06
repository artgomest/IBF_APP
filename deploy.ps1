Write-Host "Iniciando o Deploy Automático 100%..." -ForegroundColor Cyan

# Caminho absoluto do rclone para evitar erros
$rcloneExe = "C:\Users\Arthur Esteves\AppData\Local\Microsoft\WinGet\Packages\Rclone.Rclone_Microsoft.Winget.Source_8wekyb3d8bbwe\rclone-v1.74.2-windows-amd64\rclone.exe"

# 1. Obter o versionCode do app/build.gradle.kts
$gradlePath = "app\build.gradle.kts"
$gradleContent = Get-Content $gradlePath
$versionCodeMatch = [regex]::Match($gradleContent, 'versionCode\s*=\s*(\d+)')
if (-not $versionCodeMatch.Success) {
    Write-Host "Erro: Não foi possível encontrar o versionCode em $gradlePath" -ForegroundColor Red
    exit 1
}
$versionCode = [int]$versionCodeMatch.Groups[1].Value

$versionNameMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]+)"')
$versionName = if ($versionNameMatch.Success) { $versionNameMatch.Groups[1].Value } else { "1.0.0" }

Write-Host "Versão detectada: $versionName (Code: $versionCode)" -ForegroundColor Yellow

# 2. Compilar o APK
Write-Host "Compilando o APK (assembleRelease)..." -ForegroundColor Cyan
.\gradlew assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erro durante a compilação do APK." -ForegroundColor Red
    exit 1
}

# 3. Fazer o Upload para o Firebase Hosting junto com o JSON
Write-Host "Copiando APK para a pasta web..." -ForegroundColor Cyan
$apkName = "ibf-app-${versionName}.apk"
Copy-Item "app\build\outputs\apk\release\app-release.apk" -Destination "web\$apkName" -Force

$linkDireto = "https://ibfapp-c4078.web.app/$apkName"
Write-Host "Link direto: $linkDireto" -ForegroundColor Green

# 4. Atualizar o version.json
$jsonPath = "web\version.json"
$jsonContent = @"
{
  "versionCode": $versionCode,
  "versionName": "$versionName",
  "linkDownload": "$linkDireto",
  "obrigatorio": true
}
"@
Set-Content -Path $jsonPath -Value $jsonContent -Encoding UTF8
Write-Host "Arquivo version.json atualizado." -ForegroundColor Green

# 5. Fazer Deploy do Firebase Hosting
Write-Host "Iniciando deploy no Firebase Hosting..." -ForegroundColor Cyan
firebase deploy --only hosting --project ibfapp-c4078
if ($LASTEXITCODE -ne 0) {
    Write-Host "Ocorreu um erro no Deploy." -ForegroundColor Red
} else {
    Write-Host "Deploy concluído com SUCESSO!" -ForegroundColor Green
}