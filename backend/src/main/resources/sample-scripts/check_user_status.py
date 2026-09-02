#!/usr/bin/env python3
"""Демонстрационный скрипт проверки статуса пользователя.

Аргумент: user_id (позиционный CLI-аргумент, без интерактивного ввода —
см. требование к скриптам для панели в generate_permissions.sh).
"""
import sys
import json
import datetime

def main():
    if len(sys.argv) < 2 or not sys.argv[1]:
        print("Ошибка: не передан параметр user_id", file=sys.stderr)
        sys.exit(1)

    user_id = sys.argv[1]
    result = {
        "user_id": user_id,
        "status": "active",
        "checked_at": datetime.datetime.utcnow().isoformat() + "Z",
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
