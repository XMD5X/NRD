# База данных — описание

СУБД: PostgreSQL 16. Схема управляется миграциями Flyway
(`backend/src/main/resources/db/migration`: `V1__extensions`, `V2__init`,
`V3__scripts_category`), запускаются автоматически при старте backend.
Расширение `pgcrypto` используется для генерации UUID (`gen_random_uuid()`).

## Таблицы

### `users`
Пользователи системы.

| Колонка | Тип | Описание |
|---|---|---|
| id | UUID PK | |
| login | varchar, unique | |
| password_hash | varchar | bcrypt-хеш (MVP-аутентификация) |
| role | varchar | `ADMIN` \| `BUSINESS_USER` |
| is_blocked | boolean | ручная блокировка администратором |
| blocked_reason | text | причина блокировки |
| created_at | timestamptz | |
| blocked_at | timestamptz | |

### `login_history`
История попыток входа (успешных и неуспешных).

| Колонка | Тип | Описание |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users, nullable | null, если логин не найден в системе |
| login_attempted | varchar | введённый логин (даже если пользователь не найден) |
| attempted_at | timestamptz | |
| success | boolean | |
| ip_address | varchar | |

### `scripts`
Зарегистрированные администратором скрипты (включая реальные адаптированные
скрипты поддержки, засеянные `DataSeeder`).

| Колонка | Тип | Описание |
|---|---|---|
| id | UUID PK | |
| name, description | varchar/text | |
| file_path | varchar | путь к файлу на диске backend |
| script_type | varchar | `BASH` \| `PYTHON` \| `POWERSHELL` |
| parameters_config | text (JSON) | описание полей формы, настраивается разработчиком |
| send_script_path | varchar, nullable | отдельный скрипт "отправки", если есть |
| visible_to_role | varchar, nullable | ограничение видимости в списке задач |
| category | varchar, nullable | группировка на UI (напр. "ГРО (Платежи в рублях)", "Генерация ЭЦП"); добавлено миграцией `V3__scripts_category.sql` |
| bank_name | varchar, nullable | банк/модуль внутри категории (напр. "СБЕР", "РАЙФ"); добавлено миграцией `V3` |
| uploaded_by | UUID FK → users | |
| uploaded_at | timestamptz | |
| active | boolean | включён/отключён администратором (в т.ч. деактивация устаревших демо-записей при переходе на новый формат имени, см. `DataSeeder.deprecateOldPermissionsDemo`) |

### `script_executions`
Каждый запуск скрипта пользователем.

| Колонка | Тип | Описание |
|---|---|---|
| id | UUID PK | |
| script_id | UUID FK → scripts | |
| user_id | UUID FK → users | кто запустил |
| parameters_json | text (JSON) | переданные значения параметров |
| status | varchar | `RUNNING` \| `GENERATED` \| `FAILED` \| `SENT` |
| result_file_path | varchar, nullable | путь к результату: **файл**, если скрипт создал один, либо **папка**, если несколько (по одному на каждое значение параметра-списка — счета, ЭЦП и т.п.); `ExecutionService.listResultFiles()` разбирает оба варианта |
| stdout, stderr | text | вывод скрипта (обрезается до 20 000 символов) |
| started_at, finished_at, sent_to_target_at | timestamptz | |

### `uploaded_files`
Файлы, загруженные бизнес-пользователем (сертификаты, счета, полномочия).

| Колонка | Тип | Описание |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users | |
| file_type | varchar | `CERTIFICATE` \| `ACCOUNT` \| `PERMISSION` \| `OTHER` |
| original_name | varchar | исходное имя файла |
| stored_path | varchar | путь на диске backend |
| uploaded_at | timestamptz | |
| related_execution_id | UUID FK → script_executions, nullable | связь с выполнением, если применимо |

### `action_history`
Общий журнал аудита действий (бизнес-события).

| Колонка | Тип | Описание |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users, nullable | |
| action_type | varchar | напр. `SCRIPT_EXECUTE`, `USER_BLOCK`, `FILE_UPLOAD` |
| entity_type, entity_id | varchar / UUID | к какой сущности относится действие |
| details | text | человекочитаемое описание |
| created_at | timestamptz | |

### `system_settings`
Простая таблица настроек ключ-значение (сейчас используется только для
уровня логирования фронтенда, ключ `frontend_log_level`).

## ER-диаграмма

Полная ER-диаграмма (Mermaid) — в `HLD.md`, раздел 4.

## Хранение файлов

Сами файлы (сертификаты, результаты скриптов) хранятся **не в БД**, а на
файловой системе сервера backend, в БД — только метаданные и путь (решение
из SA-интервью, см. `HLD.md` раздел 1 "Данные и хранение"). Каталоги по
умолчанию: `data/scripts`, `data/results/<execution_id>`, `data/uploads`,
`data/logs/frontend` (см. `application.yml`, секция `app.*`).

## Резервное копирование

Вне рамок MVP (см. `HLD.md`, раздел 9 "Риски и допущения", пункт 3). При
дальнейшем развитии рекомендуется ежедневный `pg_dump` + бэкап каталога `data/`.
