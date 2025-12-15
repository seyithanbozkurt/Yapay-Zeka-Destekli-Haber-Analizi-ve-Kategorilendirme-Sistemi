
-- ROLLER
CREATE TABLE roles (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(50) UNIQUE NOT NULL,
    description  VARCHAR(255)
);

-- KULLANICILAR
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(50) UNIQUE NOT NULL,
    email          VARCHAR(100) UNIQUE NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP
);

-- KULLANICI-ROL İLİŞKİSİ (N:N)
CREATE TABLE user_roles (
    user_id  BIGINT NOT NULL,
    role_id  INT    NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- HABER KAYNAKLARI
CREATE TABLE sources (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    base_url      VARCHAR(255),
    category_path VARCHAR(255),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- KATEGORİLER
CREATE TABLE categories (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

-- HABERLER
CREATE TABLE news (
    id            BIGSERIAL PRIMARY KEY,
    source_id     INT NOT NULL,
    external_id   VARCHAR(255),
    title         TEXT NOT NULL,
    content       TEXT,
    original_url  VARCHAR(500),
    language      VARCHAR(10) DEFAULT 'tr',
    published_at  TIMESTAMP,
    fetched_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    is_processed  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    CONSTRAINT fk_news_source
        FOREIGN KEY (source_id) REFERENCES sources(id)
);

-- HABER-KATEGORİ İLİŞKİSİ (N:N) - çoklu etiketleme için
CREATE TABLE news_categories (
    news_id     BIGINT NOT NULL,
    category_id INT    NOT NULL,
    PRIMARY KEY (news_id, category_id),
    CONSTRAINT fk_news_categories_news
        FOREIGN KEY (news_id) REFERENCES news(id),
    CONSTRAINT fk_news_categories_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- MODEL SÜRÜMLERİ
CREATE TABLE model_versions (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    CONSTRAINT fk_model_versions_user
        FOREIGN KEY (created_by) REFERENCES users(id)
);

-- SINIFLANDIRMA SONUÇLARI
CREATE TABLE news_classification_results (
    id                    BIGSERIAL PRIMARY KEY,
    news_id               BIGINT NOT NULL,
    model_version_id      INT    NOT NULL,
    predicted_category_id INT    NOT NULL,
    prediction_score      NUMERIC(5,4),
    classified_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_ncr_news
        FOREIGN KEY (news_id) REFERENCES news(id),
    CONSTRAINT fk_ncr_model_version
        FOREIGN KEY (model_version_id) REFERENCES model_versions(id),
    CONSTRAINT fk_ncr_category
        FOREIGN KEY (predicted_category_id) REFERENCES categories(id)
);

-- KULLANICI GERİ BİLDİRİMLERİ
CREATE TABLE user_feedback (
    id                           BIGSERIAL PRIMARY KEY,
    news_id                      BIGINT NOT NULL,
    user_id                      BIGINT NOT NULL,
    model_version_id             INT,
    current_predicted_category_id INT,
    user_selected_category_id    INT NOT NULL,
    feedback_type                VARCHAR(20) NOT NULL, -- CORRECT / WRONG / RELABELED vb.
    comment                      TEXT,
    created_at                   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_feedback_news
        FOREIGN KEY (news_id) REFERENCES news(id),
    CONSTRAINT fk_feedback_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_feedback_model_version
        FOREIGN KEY (model_version_id) REFERENCES model_versions(id),
    CONSTRAINT fk_feedback_current_cat
        FOREIGN KEY (current_predicted_category_id) REFERENCES categories(id),
    CONSTRAINT fk_feedback_user_cat
        FOREIGN KEY (user_selected_category_id) REFERENCES categories(id)
);

-- TARAYICI / GÖREV LOG KAYITLARI
CREATE TABLE crawling_logs (
    id            BIGSERIAL PRIMARY KEY,
    source_id     INT NOT NULL,
    started_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at   TIMESTAMP,
    status        VARCHAR(20) NOT NULL, -- SUCCESS / FAILED / PARTIAL
    fetched_count INT,
    error_message TEXT,
    CONSTRAINT fk_crawling_logs_source
        FOREIGN KEY (source_id) REFERENCES sources(id)
);

--  İNDEKSLER

CREATE INDEX idx_news_source_id       ON news(source_id);
CREATE INDEX idx_news_published_at    ON news(published_at);
CREATE INDEX idx_ncr_news_active      ON news_classification_results(news_id, is_active);
CREATE INDEX idx_feedback_news        ON user_feedback(news_id);
CREATE INDEX idx_feedback_user        ON user_feedback(user_id);
CREATE INDEX idx_crawling_logs_source ON crawling_logs(source_id);


