-- 임시 완충: 완제품 입고 등록에서 창고/수량 누락 시에도 저장되도록 NULL 허용
-- 운영에서는 NOT NULL로 복구 예정이므로 후속 점검 필요
ALTER TABLE fg_inbounds MODIFY warehouse_code varchar(20) NULL;
ALTER TABLE fg_inbounds MODIFY inbound_qty decimal(18,3) NULL;