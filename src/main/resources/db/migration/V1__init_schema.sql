-- =============================================================
-- V1 — Initial Schema
-- Reconciliation Engine
-- =============================================================
-- Spring Batch 5 metadata tables are created here so Flyway owns
-- ALL schema changes. This ensures reproducible deployments.
-- =============================================================

-- ---------------------------------------------------------------
-- SPRING BATCH METADATA TABLES
-- (Copied from spring-batch-core schema-postgresql.sql)
-- ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT       NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100) NOT NULL,
    JOB_KEY         VARCHAR(32)  NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION           BIGINT,
    JOB_INSTANCE_ID   BIGINT       NOT NULL,
    CREATE_TIME       TIMESTAMP    NOT NULL,
    START_TIME        TIMESTAMP    DEFAULT NULL,
    END_TIME          TIMESTAMP    DEFAULT NULL,
    STATUS            VARCHAR(10),
    EXIT_CODE         VARCHAR(2500),
    EXIT_MESSAGE      VARCHAR(2500),
    LAST_UPDATED      TIMESTAMP,
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID)
        REFERENCES BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT       NOT NULL,
    PARAMETER_NAME   VARCHAR(100) NOT NULL,
    PARAMETER_TYPE   VARCHAR(100) NOT NULL,
    PARAMETER_VALUE  VARCHAR(2500),
    IDENTIFYING      CHAR(1)      NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID  BIGINT       NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    CREATE_TIME        TIMESTAMP    NOT NULL,
    START_TIME         TIMESTAMP    DEFAULT NULL,
    END_TIME           TIMESTAMP    DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    WRITE_SKIP_COUNT   BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       TIMESTAMP,
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID)
        REFERENCES BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE SEQUENCE IF NOT EXISTS BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_EXECUTION_SEQ  MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS BATCH_JOB_SEQ             MAXVALUE 9223372036854775807 NO CYCLE;

-- ---------------------------------------------------------------
-- BUSINESS DOMAIN TABLES
-- ---------------------------------------------------------------

-- Raw transactions ingested from CSV files
CREATE TABLE transactions (
    id                  BIGSERIAL     PRIMARY KEY,
    external_id         VARCHAR(64)   NOT NULL UNIQUE,
    source              VARCHAR(50)   NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'USD',
    transaction_date    DATE          NOT NULL,
    description         VARCHAR(255),
    processing_status   VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_external_id    ON transactions (external_id);
CREATE INDEX idx_transactions_processing_status ON transactions (processing_status);
CREATE INDEX idx_transactions_date           ON transactions (transaction_date);

-- Internal ledger entries (the "truth" we reconcile against)
CREATE TABLE ledger_entries (
    id               BIGSERIAL    PRIMARY KEY,
    reference        VARCHAR(64)  NOT NULL UNIQUE,
    amount           NUMERIC(19,4) NOT NULL,
    currency         VARCHAR(3)   NOT NULL DEFAULT 'USD',
    entry_date       DATE         NOT NULL,
    account_code     VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_reference ON ledger_entries (reference);
CREATE INDEX idx_ledger_date      ON ledger_entries (entry_date);

-- Results of the reconciliation job
CREATE TABLE reconciliation_results (
    id                   BIGSERIAL     PRIMARY KEY,
    transaction_id       BIGINT        NOT NULL REFERENCES transactions(id),
    ledger_entry_id      BIGINT        REFERENCES ledger_entries(id),   -- NULL if no match found
    status               VARCHAR(30)   NOT NULL,  -- MATCHED | DISCREPANCY | MISSING_IN_LEDGER
    discrepancy_amount   NUMERIC(19,4),            -- NULL when MATCHED
    notes                VARCHAR(500),
    job_execution_id     BIGINT        NOT NULL,   -- links back to Spring Batch execution
    processed_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recon_result_status       ON reconciliation_results (status);
CREATE INDEX idx_recon_result_job_exec_id  ON reconciliation_results (job_execution_id);
CREATE INDEX idx_recon_result_txn_id       ON reconciliation_results (transaction_id);