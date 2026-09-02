-- gen_random_uuid() требует расширения pgcrypto (стандартно доступно в образе postgres).
CREATE EXTENSION IF NOT EXISTS pgcrypto;
