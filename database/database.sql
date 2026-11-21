-- ============================================================
-- 1. DATABASE & CORE TABLES
-- ============================================================
CREATE DATABASE IF NOT EXISTS installment_schedule_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE installment_schedule_db;

CREATE TABLE IF NOT EXISTS users (
    user_id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('user','admin') NOT NULL DEFAULT 'user',
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_profiles (
    admin_profile_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          INT UNSIGNED NOT NULL,
    department       VARCHAR(100) NOT NULL DEFAULT 'Loan Operations',
    last_login_at    TIMESTAMP NULL DEFAULT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_profile_id),
    UNIQUE KEY uq_admin_user (user_id),
    CONSTRAINT fk_admin_profile_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loan_policies (
    policy_id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
    policy_name           VARCHAR(100) NOT NULL,
    default_interest_rate DECIMAL(5,2) NOT NULL,
    min_principal         DECIMAL(15,2) NOT NULL DEFAULT 1000.00,
    max_principal         DECIMAL(15,2) NOT NULL DEFAULT 10000000.00,
    allowed_frequency     ENUM('weekly','monthly','quarterly','yearly','all') NOT NULL DEFAULT 'all',
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by            INT UNSIGNED DEFAULT NULL,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (policy_id),
    UNIQUE KEY uq_policy_name (policy_name),
    CONSTRAINT fk_policy_admin FOREIGN KEY (updated_by)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS loans (
    loan_id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id            INT UNSIGNED NOT NULL,
    principal          DECIMAL(15,2) NOT NULL,
    interest_rate      DECIMAL(5,2)  NOT NULL,
    tenure             INT UNSIGNED  NOT NULL,
    frequency          ENUM('weekly','monthly','quarterly','yearly') NOT NULL,
    start_date         DATE NOT NULL,
    num_installments   INT UNSIGNED  NOT NULL,
    installment_amount DECIMAL(15,2) NOT NULL,
    total_payment      DECIMAL(15,2) NOT NULL,
    total_interest     DECIMAL(15,2) NOT NULL,
    status             ENUM('pending','approved','rejected','closed') NOT NULL DEFAULT 'pending',
    reviewed_by        INT UNSIGNED DEFAULT NULL,
    reviewed_at        TIMESTAMP NULL DEFAULT NULL,
    admin_remarks      VARCHAR(255) DEFAULT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (loan_id),
    KEY idx_loans_user (user_id),
    KEY idx_loans_status (status),
    CONSTRAINT fk_loans_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_loans_reviewer FOREIGN KEY (reviewed_by)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS installment_schedule (
    schedule_id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
    loan_id            INT UNSIGNED NOT NULL,
    installment_no     INT UNSIGNED NOT NULL,
    due_date           DATE NOT NULL,
    installment_amount DECIMAL(15,2) NOT NULL,
    principal_paid     DECIMAL(15,2) NOT NULL,
    interest_paid      DECIMAL(15,2) NOT NULL,
    balance            DECIMAL(15,2) NOT NULL,
    payment_status     ENUM('pending','paid') NOT NULL DEFAULT 'pending',
    PRIMARY KEY (schedule_id),
    UNIQUE KEY uq_schedule_loan_no (loan_id, installment_no),
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id)
        REFERENCES loans (loan_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    log_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    admin_id     INT UNSIGNED NOT NULL,
    action_type  VARCHAR(60) NOT NULL,
    target_table VARCHAR(60) NOT NULL,
    target_id    INT UNSIGNED DEFAULT NULL,
    old_value    TEXT DEFAULT NULL,
    new_value    TEXT DEFAULT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_audit_admin (admin_id),
    KEY idx_audit_created (created_at),
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. VIEWS FOR CLEAN QUERIES
-- ============================================================
CREATE OR REPLACE VIEW vw_user_loan_details AS
SELECT
    l.loan_id,
    l.user_id,
    l.principal,
    l.interest_rate,
    l.tenure,
    l.frequency,
    l.start_date,
    l.num_installments,
    l.installment_amount,
    l.total_payment,
    l.total_interest,
    l.status,
    l.admin_remarks,
    l.created_at
FROM loans l;

CREATE OR REPLACE VIEW vw_admin_loans_overview AS
SELECT
    l.loan_id,
    u.user_id,
    u.name AS user_name,
    u.email AS user_email,
    l.principal,
    l.interest_rate,
    l.tenure,
    l.frequency,
    l.start_date,
    l.num_installments,
    l.installment_amount,
    l.total_payment,
    l.total_interest,
    l.status,
    l.reviewed_by,
    l.reviewed_at,
    l.admin_remarks,
    l.created_at
FROM loans l
JOIN users u ON l.user_id = u.user_id;

-- ============================================================
-- 3. DATABASE ACCOUNTS & PRIVILEGES
-- ============================================================
-- Note: Use '%' instead of 'localhost' if the backend runs on another container/host
CREATE USER IF NOT EXISTS 'app_user_role'@'localhost' IDENTIFIED BY 'ChangeMe_UserPass123!';
GRANT INSERT (
    user_id, principal, interest_rate, tenure, frequency,
    start_date, num_installments, installment_amount,
    total_payment, total_interest
) ON installment_schedule_db.loans TO 'app_user_role'@'localhost';

GRANT SELECT ON installment_schedule_db.vw_user_loan_details TO 'app_user_role'@'localhost';
GRANT SELECT ON installment_schedule_db.installment_schedule TO 'app_user_role'@'localhost';
GRANT SELECT ON installment_schedule_db.loan_policies TO 'app_user_role'@'localhost';

CREATE USER IF NOT EXISTS 'app_admin_role'@'localhost' IDENTIFIED BY 'ChangeMe_AdminPass123!';
GRANT SELECT, INSERT, UPDATE, DELETE ON installment_schedule_db.* TO 'app_admin_role'@'localhost';

FLUSH PRIVILEGES;