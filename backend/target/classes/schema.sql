CREATE TABLE IF NOT EXISTS users (
    user_id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('user','admin') NOT NULL DEFAULT 'user',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email)
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
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (loan_id),
    KEY idx_loans_user (user_id),
    CONSTRAINT fk_loans_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
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
    PRIMARY KEY (schedule_id),
    UNIQUE KEY uq_schedule_loan_no (loan_id, installment_no),
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id)
        REFERENCES loans (loan_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

