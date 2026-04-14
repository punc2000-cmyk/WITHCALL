-- 119비상벨 / 돌봄비상벨 연락처 등록여부 분리
ALTER TABLE devices ADD COLUMN contact_1_status_119  TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_2_status_119  TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_3_status_119  TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_4_status_119  TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_5_status_119  TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_1_status_care TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_2_status_care TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_3_status_care TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_4_status_care TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_5_status_care TEXT DEFAULT '';
