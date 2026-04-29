-- ============================================================
--  Muhasebe Backend - PostgreSQL Schema Init
--  DB: muhasebedb
--  User: postgres / postgres
-- ============================================================

-- ============ STATIK TABLOLAR (once bunlar - FK hedefi) ============

CREATE TABLE IF NOT EXISTS category (
                                        id                 BIGSERIAL PRIMARY KEY,
                                        name               VARCHAR(255) NOT NULL,
    description        VARCHAR(500),
    parent_id          BIGINT REFERENCES category(id) ON DELETE SET NULL,
    created_by         VARCHAR(100),
    created_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            INTEGER DEFAULT 0
    );

CREATE INDEX IF NOT EXISTS idx_category_parent ON category(parent_id);

CREATE TABLE IF NOT EXISTS category_details (
                                                id                 BIGSERIAL PRIMARY KEY,
                                                name               VARCHAR(255) NOT NULL,
    data_type          VARCHAR(50) DEFAULT 'string',
    created_by         VARCHAR(100),
    created_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            INTEGER DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS category_detail_map (
    category_id BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    detail_id   BIGINT NOT NULL REFERENCES category_details(id) ON DELETE CASCADE,
    PRIMARY KEY (category_id, detail_id)
    );

CREATE TABLE IF NOT EXISTS product (
                                       id                 BIGSERIAL PRIMARY KEY,
                                       name               VARCHAR(255) NOT NULL,
    description        VARCHAR(1000),
    price              NUMERIC(19,4) DEFAULT 0,
    stock              INTEGER DEFAULT 0,
    category_id        BIGINT REFERENCES category(id) ON DELETE SET NULL,
    created_by         VARCHAR(100),
    created_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP,
    version            INTEGER DEFAULT 0
    );

CREATE INDEX IF NOT EXISTS idx_product_category_id ON product(category_id);

-- ============ METADATA TABLOLARI ============

-- Dinamik olarak olusturulan tablolarin listesi
CREATE TABLE IF NOT EXISTS dynamic_tables (
                                              id           SERIAL PRIMARY KEY,
                                              table_name   VARCHAR(255) NOT NULL UNIQUE,
    category_id  BIGINT REFERENCES category(id) ON DELETE SET NULL,
    created_by   VARCHAR(100) DEFAULT 'system',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version      INTEGER DEFAULT 1
    );

CREATE INDEX IF NOT EXISTS idx_dynamic_tables_category ON dynamic_tables(category_id);

-- Enum kolonlarin deger listesi (durum: odenmedi/odendi gibi)
CREATE TABLE IF NOT EXISTS column_enum_map (
                                               id            SERIAL PRIMARY KEY,
                                               table_name    VARCHAR(255) NOT NULL,
    column_name   VARCHAR(255) NOT NULL,
    enum_values   TEXT NOT NULL,
    default_value VARCHAR(255),
    created_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (table_name, column_name)
    );

CREATE INDEX IF NOT EXISTS idx_column_enum_map_table ON column_enum_map(table_name);

-- Tablo iliskileri haritasi (one-to-many, many-to-many)
-- Tablo iliskileri haritasi (one-to-many, many-to-many)
CREATE TABLE IF NOT EXISTS table_map (
                                         id                  SERIAL PRIMARY KEY,
                                         table_name          VARCHAR(255) NOT NULL,
    related_table       VARCHAR(255) NOT NULL,
    relation_type       VARCHAR(50)  NOT NULL,
    relation_colum_name VARCHAR(255),
    join_table_name     VARCHAR(255),
    fk_column_name      VARCHAR(255),
    created_by          VARCHAR(100) DEFAULT 'system',
    created_date        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_by    VARCHAR(100),
    last_modified_date  TIMESTAMP,
    version             INTEGER DEFAULT 1
    );

CREATE INDEX IF NOT EXISTS idx_table_map_table_name ON table_map(table_name);
CREATE INDEX IF NOT EXISTS idx_table_map_related_table ON table_map(related_table);

-- View kayitlari
CREATE TABLE IF NOT EXISTS view_map (
                                        id                  SERIAL PRIMARY KEY,
                                        view_name           VARCHAR(255) NOT NULL UNIQUE,
    relation_table_name VARCHAR(500),
    created_date        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Procedure kayitlari
CREATE TABLE IF NOT EXISTS dynamic_procedures (
                                                  id              SERIAL PRIMARY KEY,
                                                  procedure_name  VARCHAR(255) NOT NULL UNIQUE,
    definition_json TEXT,
    created_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Sistem loglari (SystemLogEntity icin)
CREATE TABLE IF NOT EXISTS sistem_loglari (
                                              id        SERIAL PRIMARY KEY,
                                              islem_adi VARCHAR(255),
    detay     TEXT,
    tarih     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );



-- ============ AUTHENTICATION ============
CREATE TABLE IF NOT EXISTS app_users (
                                         id            BIGSERIAL PRIMARY KEY,
                                         username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,    -- BCrypt hash
    role          VARCHAR(50)  NOT NULL,    -- ADMIN | KULLANICI
    full_name     VARCHAR(255),
    created_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_app_users_username ON app_users(username);

-- Default admin kullanicisi: admin / admin123
-- BCrypt hash 'admin123' icin asagidaki - cost factor 10
INSERT INTO app_users (username, password_hash, role, full_name)
VALUES ('admin', '$2a$10$lO8W8aSvx1PRp1rzfD6jNu7PLT4g9OvhQtWDW0ukPRWU/MK.8rQiO', 'ADMIN', 'Sistem Yoneticisi')
    ON CONFLICT (username) DO NOTHING;