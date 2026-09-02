-- Группировка задач по категориям ("эталонным ролям") и банкам —
-- для задачи "Выдача прав доступа на счета для эталонных ролей".
-- Оба поля nullable: старые демо-скрипты остаются плоским списком без категории.

ALTER TABLE scripts ADD COLUMN category VARCHAR(255);
ALTER TABLE scripts ADD COLUMN bank_name VARCHAR(255);

CREATE INDEX idx_scripts_category ON scripts(category);
