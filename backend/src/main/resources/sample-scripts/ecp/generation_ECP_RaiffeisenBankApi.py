import sys
import os
import json
from datetime import datetime
import traceback  # Для вывода трассировки ошибок


def normalize_input(user_input):
    """Удаление скрытых и специальных символов из входных данных."""
    normalized = user_input.strip()
    # Удаляем лишние невидимые символы, такие как неразрывные пробелы
    normalized = ''.join([char for char in normalized if char.isprintable()])
    return normalized


def split_list(raw):
    """Разбирает значение параметра панели (список через запятую, в одном порядке для
    комментариев/номеров/дат) в список нормализованных строк. Пустые элементы
    (например, из-за лишней запятой в конце) отбрасываются."""
    if raw is None:
        return []
    return [normalize_input(part) for part in raw.split(',') if normalize_input(part)]


def validate_date(date_str):
    """Проверка формата даты и преобразование в нужный формат ISO."""
    try:
        # Парсим дату из нормализованного формата dd.mm.yyyy
        date_obj = datetime.strptime(date_str, "%d.%m.%Y")
        return date_obj.strftime("%Y-%m-%dT00:00:00.000")
    except ValueError:
        raise ValueError(f"Некорректный формат даты '{date_str}'. Используйте формат ДД.ММ.ГГГГ.")


def save_json(data, file_path):
    """Сохранение JSON-файла с дополнительной защитой от ошибок."""
    try:
        with open(file_path, "w", encoding="utf-8") as file:
            json.dump(data, file, ensure_ascii=False, indent=4)
        print(f"Запрос успешно сохранён в файле: {file_path}")
    except Exception as e:
        print(f"Произошла ошибка при сохранении файла '{file_path}': {str(e)}")
        traceback.print_exc()


def main():
    # Параметры запуска (передаются панелью как позиционные аргументы командной строки,
    # без интерактивного ввода input() — панель запускает скрипт неинтерактивно и не
    # сможет ответить на приглашения ввода):
    #   argv[1] — комментарии, через запятую
    #   argv[2] — номера сертификатов ЭЦП, через запятую
    #   argv[3] — даты истечения (ДД.ММ.ГГГГ), через запятую
    # Значение №N в каждом списке образует одну ЭЦП -> один JSON-файл результата.
    # Результат сохраняется в текущую рабочую директорию (её задаёт панель под каждый запуск).
    comments_raw = sys.argv[1] if len(sys.argv) > 1 else ""
    numbers_raw = sys.argv[2] if len(sys.argv) > 2 else ""
    dates_raw = sys.argv[3] if len(sys.argv) > 3 else ""

    comments = split_list(comments_raw)
    numbers_ecp = split_list(numbers_raw)
    exp_dates_ecp = split_list(dates_raw)

    save_path = os.getcwd()

    # Список для сбора ошибок
    errors = []

    # Проверка соответствия количества введённых данных
    if len(comments) != len(numbers_ecp) or len(numbers_ecp) != len(exp_dates_ecp):
        errors.append("Количество введённых комментариев, номеров сертификатов и дат истечения не совпадает!")

    success_count = 0
    error_count = 0

    # Создание и запись JSON-документов — по одному файлу на каждую ЭЦП
    for i in range(max(len(comments), len(numbers_ecp), len(exp_dates_ecp))):
        try:
            comment = comments[i]
            number_ecp = numbers_ecp[i].upper()  # Преобразование номера сертификата в верхний регистр
            exp_date_ecp = exp_dates_ecp[i]

            # Преобразование даты в нужный формат
            formatted_exp_date = validate_date(exp_date_ecp)

            # Формирование JSON-данных
            json_body = {
                "is_client": True,
                "location": "CurrentUser",
                "store": "My",
                "provider": "CryptoPro",
                "serial_number": number_ecp,
                "valid_to_date": formatted_exp_date,
                "description": comment,
                "bank_modules": ["RaiffeisenBankApi"],
                "is_primary": False
            }

            # Генерация имени файла (убираем пробелы и специальные символы)
            file_name = f"{comment.replace(' ', '_')}_{number_ecp}_{formatted_exp_date}.json".replace(':', '')
            file_path = os.path.join(save_path, file_name)

            # Сохранение JSON-файла
            save_json(json_body, file_path)
            success_count += 1

        except IndexError:
            errors.append(f"Не хватает данных для записи строки №{i + 1}")
            error_count += 1
        except Exception as e:
            errors.append(f"Ошибка обработки строки №{i + 1}: {str(e)}")
            error_count += 1
            traceback.print_exc()

    # Итоговая статистика
    print(f"\nОбработано строк:")
    print(f"Успешно: {success_count}")
    print(f"С ошибкой: {error_count}\n")

    # Вывод ошибок, если они были
    if errors:
        print("Список ошибок:")
        for idx, err in enumerate(errors, start=1):
            print(f"{idx}) {err}")

    # Если не сгенерировано ни одного файла — считаем выполнение неуспешным
    if success_count == 0:
        sys.exit(1)


if __name__ == "__main__":
    main()
