-- 기존 테이블 제거 후 재생성 (데이터 초기화 포함)
DROP TABLE IF EXISTS devices;
DROP TABLE IF EXISTS batches;

CREATE TABLE batches (
    id          INTEGER  PRIMARY KEY AUTOINCREMENT,
    name        TEXT     NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_count INTEGER  DEFAULT 0
);

CREATE TABLE devices (
    id              INTEGER  PRIMARY KEY AUTOINCREMENT,
    batch_id        INTEGER  REFERENCES batches(id),
    region          TEXT     DEFAULT '',
    school_name     TEXT     DEFAULT '',
    address         TEXT     DEFAULT '',
    phone_119       TEXT     DEFAULT '',
    registered_119  TEXT     DEFAULT '',
    phone_care      TEXT     DEFAULT '',
    registered_care TEXT     DEFAULT '',
    contact_1       TEXT     DEFAULT '',
    contact_2       TEXT     DEFAULT '',
    contact_3       TEXT     DEFAULT '',
    contact_4       TEXT     DEFAULT '',
    contact_5       TEXT     DEFAULT '',
    status          TEXT     DEFAULT 'pending',
    completed_at    DATETIME
);

CREATE INDEX idx_devices_batch_status ON devices(batch_id, status);
