# Параметры запуска (передаются панелью как позиционные аргументы командной строки,
# без интерактивного ввода Read-Host)
param(
    [string]$userId,
    [string]$accounts
)
# Результат сохраняется в текущую рабочую директорию (её задаёт панель под каждый запуск)
$savePath = (Get-Location).Path

# Проверяем существование папки назначения
if (-not(Test-Path $savePath)) {
    New-Item -ItemType Directory -Path $savePath | Out-Null
}

# Преобразуем строку номеров счетов в массивcl
$accountArray = $accounts.Split(',')

foreach ($account in $accountArray)
{
    # Формируем тело JSON-запроса
    $jsonBody = @"
{"documents_permissions": [{"document_type":"Payment","permissions":["View"],"phases":[]},{"document_type":"PaymentStatusRequest","permissions":[],"phases":[]},{"document_type":"Statement","permissions":["View"],"phases":[]},{"document_type":"StatementRequest","permissions":["View"],"phases":[]},{"document_type":"BankLetter","permissions":["View","AttachmentDownload"],"phases":[]},{"document_type":"CurrencyContract","permissions":["View","AttachmentDownload"],"phases":[]},{"document_type":"Svo","permissions":["View","AttachmentDownload"],"phases":[]},{"document_type":"Spd","permissions":["View","AttachmentDownload"],"phases":[]},{"document_type":"Payroll","permissions":["View"],"phases":[]},{"document_type":"Notice","permissions":["View"],"phases":[]},{"document_type":"BankControlStatementInfo","permissions":["View"],"phases":[]},{"document_type":"BankControlStatement","permissions":["View","AttachmentDownload"],"phases":[]},{"document_type":"DepositOffer","permissions":["View"],"phases":[]}],"user_id": "$userId","account_number": "$($account.Trim())","bank_module": "CreditEurope"}
"@

    # Убираем спецсимволы из имени файла
    $sanitizedFileName = $account.Replace(':', '_').Trim()
    
    # Создаем уникальный файл для каждого счёта
    $fileName = Join-Path -Path $savePath -ChildPath ("request_" + $sanitizedFileName + ".json")
    Set-Content -Path $fileName -Value $jsonBody

    Write-Output "Файл '$fileName' успешно создан."
}
