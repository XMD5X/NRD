-- Начальная схема БД. См. HLD.md, раздел 4 "Модель данных".

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    login VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    blocked_at TIMESTAMPTZ
);

CREATE TABLE login_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    login_attempted VARCHAR(255) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(64)
);
CREATE INDEX idx_login_history_user ON login_history(user_id);

CREATE TABLE scripts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    file_path VARCHAR(1024) NOT NULL,
    script_type VARCHAR(32) NOT NULL,
    parameters_config TEXT NOT NULL DEFAULT '[]',
    send_script_path VARCHAR(1024),
    visible_to_role VARCHAR(32),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE script_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    script_id UUID NOT NULL REFERENCES scripts(id),
    user_id UUID NOT NULL REFERENCES users(id),
    parameters_json TEXT,
    status VARCHAR(32) NOT NULL,
    result_file_path VARCHAR(1024),
    stdout TEXT,
    stderr TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    sent_to_target_at TIMESTAMPTZ
);
CREATE INDEX idx_executions_user ON script_executions(user_id);
CREATE INDEX idx_executions_script ON script_executions(script_id);

CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    file_type VARCHAR(32) NOT NULL,
    original_name VARCHAR(512) NOT NULL,
    stored_path VARCHAR(1024) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    related_execution_id UUID REFERENCES script_executions(id)
);
CREATE INDEX idx_uploaded_files_user ON uploaded_files(user_id);

CREATE TABLE action_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id UUID,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_action_history_user ON action_history(user_id);
CREATE INDEX idx_action_history_created ON action_history(created_at);

CREATE TABLE system_settings (
    setting_key VARCHAR(128) PRIMARY KEY,
    setting_value VARCHAR(512) NOT NULL
);
