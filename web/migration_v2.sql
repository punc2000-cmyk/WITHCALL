-- 연락처별 등록여부 컬럼 추가 (기존 데이터 유지)
ALTER TABLE devices ADD COLUMN contact_1_status TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_2_status TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_3_status TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_4_status TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN contact_5_status TEXT DEFAULT '';
