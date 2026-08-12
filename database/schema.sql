-- =====================================================================
-- AI Resume Optimizer - MySQL 8.x schema
-- Reference DDL matching the JPA entities. With JPA ddl-auto=update the
-- tables are created automatically; this file documents the production
-- layout and can be applied manually for managed environments.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS resume_optimizer
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE resume_optimizer;

-- ---------------------------------------------------------------------
-- Users & Roles (RBAC)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(30)  NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    first_name           VARCHAR(60)  NOT NULL,
    last_name            VARCHAR(60)  NOT NULL,
    email                VARCHAR(120) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,               -- BCrypt hash
    profile_picture_path VARCHAR(255),
    email_verified       BIT(1)       NOT NULL DEFAULT b'0',
    account_locked       BIT(1)       NOT NULL DEFAULT b'0',
    last_login_at        DATETIME(6),
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    INDEX idx_users_created (created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Authentication
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS otp_verifications (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(120) NOT NULL,
    code       VARCHAR(10) NOT NULL,
    type       VARCHAR(20) NOT NULL,                          -- EMAIL_VERIFICATION | PASSWORD_RESET
    expires_at DATETIME(6) NOT NULL,
    used       BIT(1)      NOT NULL DEFAULT b'0',
    attempts   INT         NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_otp_email (email),
    INDEX idx_otp_code_type (code, type)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    token_hash             VARCHAR(64)  NOT NULL UNIQUE,      -- SHA-256 of the token
    expires_at             DATETIME(6)  NOT NULL,
    revoked                BIT(1)       NOT NULL DEFAULT b'0',
    replaced_by_token_hash VARCHAR(64),
    created_at             DATETIME(6)  NOT NULL,
    revoked_at             DATETIME(6),
    INDEX idx_refresh_token_user (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Resumes & versions
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resumes (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    name              VARCHAR(120) NOT NULL,
    content           LONGTEXT,
    structure         LONGTEXT,                               -- JSON sections
    original_file_name VARCHAR(255),
    storage_path      VARCHAR(500),
    file_type         VARCHAR(10),
    file_size_bytes   BIGINT,
    template          VARCHAR(40)  DEFAULT 'modern',
    is_favorite       BIT(1)       NOT NULL DEFAULT b'0',
    optimized         BIT(1)       NOT NULL DEFAULT b'0',
    ats_score         INT,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    INDEX idx_resume_user (user_id),
    INDEX idx_resume_name (name),
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS resume_versions (
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resume_id              BIGINT       NOT NULL,
    name                   VARCHAR(120) NOT NULL,
    label                  VARCHAR(255),
    content                LONGTEXT,
    structure              LONGTEXT,
    ats_score              INT,
    keyword_match_percent  INT,
    ai_generated           BIT(1)       NOT NULL DEFAULT b'0',
    is_favorite            BIT(1)       NOT NULL DEFAULT b'0',
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    INDEX idx_version_resume (resume_id),
    INDEX idx_version_created (created_at),
    CONSTRAINT fk_version_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Job descriptions
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_descriptions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    title               VARCHAR(160),
    content             LONGTEXT     NOT NULL,
    skills              JSON,
    responsibilities    JSON,
    keywords            JSON,
    experience          JSON,
    education           JSON,
    tools               JSON,
    source              VARCHAR(20)  NOT NULL,                -- PASTE | UPLOAD
    original_file_name  VARCHAR(255),
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    INDEX idx_jd_user (user_id),
    INDEX idx_jd_title (title),
    CONSTRAINT fk_jd_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Optimization history & downloads
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS optimization_history (
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    resume_id              BIGINT,
    job_description_id     BIGINT,
    result_version_id      BIGINT,
    ats_score              INT,
    keyword_match_percent  INT,
    missing_keywords       JSON,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    ai_provider            VARCHAR(20),
    duration_ms            BIGINT,
    completed_at           DATETIME(6),
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    INDEX idx_hist_user (user_id),
    INDEX idx_hist_created (created_at),
    CONSTRAINT fk_hist_user    FOREIGN KEY (user_id)            REFERENCES users (id)           ON DELETE CASCADE,
    CONSTRAINT fk_hist_resume  FOREIGN KEY (resume_id)          REFERENCES resumes (id)         ON DELETE SET NULL,
    CONSTRAINT fk_hist_jd      FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id) ON DELETE SET NULL,
    CONSTRAINT fk_hist_version FOREIGN KEY (result_version_id)  REFERENCES resume_versions (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS downloads (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    resume_id  BIGINT,
    version_id BIGINT,
    format     VARCHAR(10) NOT NULL,                          -- PDF | DOCX
    file_name  VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_download_user (user_id),
    CONSTRAINT fk_download_user    FOREIGN KEY (user_id)    REFERENCES users (id)         ON DELETE CASCADE,
    CONSTRAINT fk_download_resume  FOREIGN KEY (resume_id)  REFERENCES resumes (id)       ON DELETE SET NULL,
    CONSTRAINT fk_download_version FOREIGN KEY (version_id) REFERENCES resume_versions (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS stored_files (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    category      VARCHAR(20)  NOT NULL,                     -- resumes | jd | profile
    original_name VARCHAR(255),
    content_type  VARCHAR(100),
    data          LONGBLOB     NOT NULL,                     -- file bytes (DB storage mode)
    created_at    DATETIME(6)  NOT NULL,
    INDEX idx_stored_user (user_id),
    CONSTRAINT fk_stored_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Seed roles
-- ---------------------------------------------------------------------
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN')
ON DUPLICATE KEY UPDATE name = VALUES(name);
