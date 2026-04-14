-- ══════════════════════════════════════════════════════════════
-- WITHCALL DB 스키마 (엑셀 FORM: D:\1.xlsx 기준)
--
-- 엑셀 열 매핑
--   A  지역          → region
--   B  학교명        → school_name
--   C  주소          → address
--   D  119비상벨     → phone_119
--   E  등록여부(119)  → registered_119
--   F  돌봄비상벨    → phone_care
--   G  등록여부(돌봄) → registered_care
--   H  비상연락처1   → contact_1
--   I  비상연락처2   → contact_2
--   J  비상연락처3   → contact_3
--   K  비상연락처4   → contact_4
--   L  비상연락처5   → contact_5
--
-- "해당없음" / "해당 없음" 값은 빈 문자열('')로 저장
-- ══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS batches (
    id          INTEGER  PRIMARY KEY AUTOINCREMENT,
    name        TEXT     NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_count INTEGER  DEFAULT 0
);

CREATE TABLE IF NOT EXISTS devices (
    id              INTEGER  PRIMARY KEY AUTOINCREMENT,
    batch_id        INTEGER  REFERENCES batches(id),

    -- 학교 정보 (A~C)
    region          TEXT     DEFAULT '',   -- A: 지역
    school_name     TEXT     DEFAULT '',   -- B: 학교명
    address         TEXT     DEFAULT '',   -- C: 주소

    -- 119비상벨 (D~E)
    phone_119       TEXT     DEFAULT '',   -- D: 119비상벨 전화번호
    registered_119  TEXT     DEFAULT '',   -- E: 등록여부

    -- 돌봄비상벨 (F~G)
    phone_care      TEXT     DEFAULT '',   -- F: 돌봄비상벨 전화번호
    registered_care TEXT     DEFAULT '',   -- G: 등록여부

    -- 긴급 문자 발송 번호 (H~L)
    contact_1       TEXT     DEFAULT '',   -- H: 비상연락처1(관리자)
    contact_2       TEXT     DEFAULT '',   -- I: 비상연락처2(관리자)
    contact_3       TEXT     DEFAULT '',   -- J: 비상연락처3(관리자)
    contact_4       TEXT     DEFAULT '',   -- K: 비상연락처4(관리자)
    contact_5       TEXT     DEFAULT '',   -- L: 비상연락처5(관리자)

    -- 처리 상태
    status          TEXT     DEFAULT 'pending',
    completed_at    DATETIME
);

CREATE INDEX IF NOT EXISTS idx_devices_batch_status ON devices(batch_id, status);
