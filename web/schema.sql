CREATE TABLE IF NOT EXISTS batches (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_count INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS devices (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_id     INTEGER REFERENCES batches(id),
    phone_number TEXT    NOT NULL,
    msg_b        TEXT    DEFAULT '',
    msg_c        TEXT    DEFAULT '',
    msg_d        TEXT    DEFAULT '',
    msg_e        TEXT    DEFAULT '',
    msg_f        TEXT    DEFAULT '',
    status       TEXT    DEFAULT 'pending',
    completed_at DATETIME
);

CREATE INDEX IF NOT EXISTS idx_devices_batch_status ON devices(batch_id, status);
