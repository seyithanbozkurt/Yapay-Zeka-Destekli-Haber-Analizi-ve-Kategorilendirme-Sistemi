-- PostgreSQL: Savaş kategorisini categories tablosuna ekler (yoksa).
-- Proje tek veritabanı: PostgreSQL (haberdb, localhost:5432).
--
-- Çalıştırmak:
--   psql -U postgres -d haberdb -f scripts/add-savas-category.sql
-- veya pgAdmin / DBeaver ile haberdb bağlanıp bu dosyayı çalıştırın.

-- Yöntem 1: name alanında UNIQUE constraint varsa (JPA ile oluşan tabloda vardır)
INSERT INTO categories (name, description, is_active)
VALUES ('Savaş', 'Savaş, çatışma ve savunma haberleri kategorisi', true)
ON CONFLICT (name) DO NOTHING;

-- Yöntem 2: ON CONFLICT hatası alırsanız, aşağıdaki satırı tek başına kullanın:
-- INSERT INTO categories (name, description, is_active)
-- SELECT 'Savaş', 'Savaş, çatışma ve savunma haberleri kategorisi', true
-- WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Savaş');
