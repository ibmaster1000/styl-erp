ALTER TABLE material_orders
    DROP COLUMN IF EXISTS delivery_place;

ALTER TABLE material_orders
    ADD COLUMN IF NOT EXISTS color_type VARCHAR(20) DEFAULT 'COLOR',
    ADD COLUMN IF NOT EXISTS vendor_type VARCHAR(20) DEFAULT 'CUSTOMER',
    ADD COLUMN IF NOT EXISTS vendor_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS warehouse_type VARCHAR(20) DEFAULT 'WAREHOUSE',
    ADD COLUMN IF NOT EXISTS warehouse_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS unit_price DECIMAL(18, 2);

UPDATE material_orders
SET vendor_code = COALESCE(vendor_code, supplier_code),
    vendor_type = COALESCE(vendor_type, 'CUSTOMER'),
    warehouse_type = COALESCE(warehouse_type, 'WAREHOUSE'),
    color_type = COALESCE(color_type, 'COLOR')
WHERE vendor_code IS NULL OR vendor_type IS NULL OR warehouse_type IS NULL OR color_type IS NULL;

ALTER TABLE material_orders
    ADD CONSTRAINT fk_material_orders_vendor
        FOREIGN KEY (vendor_type, vendor_code)
        REFERENCES codes(code_type, code),
    ADD CONSTRAINT fk_material_orders_warehouse
        FOREIGN KEY (warehouse_type, warehouse_code)
        REFERENCES codes(code_type, code),
    ADD CONSTRAINT fk_material_orders_color
        FOREIGN KEY (color_type, color_code)
        REFERENCES codes(code_type, code);

CREATE TABLE IF NOT EXISTS material_inbounds (
    inbound_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    m_order_code VARCHAR(64) NOT NULL,
    received_qty DECIMAL(18, 3) NOT NULL,
    received_datetime DATETIME NOT NULL,
    created_by VARCHAR(50),
    remark VARCHAR(255),
    CONSTRAINT fk_material_inbounds_order
        FOREIGN KEY (m_order_code)
        REFERENCES material_orders(m_order_code)
);

CREATE TABLE IF NOT EXISTS material_outbounds (
    outbound_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    m_order_code VARCHAR(64) NOT NULL,
    planned_out_qty DECIMAL(18, 3) NOT NULL,
    issued_out_qty DECIMAL(18, 3) NOT NULL,
    producer_type VARCHAR(20) NOT NULL,
    producer_code VARCHAR(50) NOT NULL,
    outbound_datetime DATETIME NOT NULL,
    remark VARCHAR(255),
    CONSTRAINT fk_material_outbounds_order
        FOREIGN KEY (m_order_code)
        REFERENCES material_orders(m_order_code),
    CONSTRAINT fk_material_outbounds_producer
        FOREIGN KEY (producer_type, producer_code)
        REFERENCES codes(code_type, code)
);

DROP TABLE IF EXISTS material_transactions;
