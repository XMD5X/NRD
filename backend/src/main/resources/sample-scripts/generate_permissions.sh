#!/usr/bin/env bash
# Демонстрационный скрипт: генерация JSON-запроса на настройку прав доступа
# к документам по банковским счетам для указанного пользователя.
#
# Это переработанная под неинтерактивный запуск версия реального скрипта
# поддержки (PowerShell), полученного от заказчика при сборе требований
# (см. REFINED_VISION.md). Оригинал использовал Read-Host (интерактивный
# ввод) — для запуска из панели скрипт должен принимать параметры как
# позиционные CLI-аргументы, что и сделано здесь.
#
# Аргументы:
#   $1 - user_id
#   $2 - accounts (номера счетов через запятую)
#
# Результат: JSON-файлы request_<счёт>.json в текущей рабочей директории
# (панель сама передаёт актуальную рабочую директорию под каждый запуск).

set -euo pipefail

USER_ID="${1:-}"
ACCOUNTS="${2:-}"

if [ -z "$USER_ID" ] || [ -z "$ACCOUNTS" ]; then
    echo "Ошибка: не переданы обязательные параметры user_id и accounts" >&2
    exit 1
fi

IFS=',' read -ra ACCOUNT_ARRAY <<< "$ACCOUNTS"

for account in "${ACCOUNT_ARRAY[@]}"; do
    account_trimmed=$(echo "$account" | xargs)
    sanitized=$(echo "$account_trimmed" | tr ':' '_')
    filename="request_${sanitized}.json"

    cat > "$filename" <<JSON
{
  "documents_permissions": [
    {"document_type": "Payment", "permissions": ["View"], "phases": []},
    {"document_type": "PaymentStatusRequest", "permissions": [], "phases": []},
    {"document_type": "Statement", "permissions": ["View"], "phases": []},
    {"document_type": "StatementRequest", "permissions": ["Import", "View", "Delete", "Cancel"], "phases": []},
    {"document_type": "BankLetter", "permissions": ["Import", "View", "AttachmentDownload"], "phases": []},
    {"document_type": "CurrencyContract", "permissions": ["View"], "phases": []}
  ],
  "user_id": "${USER_ID}",
  "account_number": "${account_trimmed}",
  "bank_module": "Unicredit"
}
JSON

    echo "Файл '$filename' успешно создан."
done

echo "Генерация завершена. Пользователь: ${USER_ID}, счетов обработано: ${#ACCOUNT_ARRAY[@]}."
