-- =============================================================
-- V2 — Seed ledger entries for local development and testing
-- =============================================================
-- These entries are the "source of truth" that the reconciliation
-- engine checks incoming transactions against.
--
-- Matches sample_transactions.csv:
--   TXN-001 → exact match     (MATCHED)
--   TXN-002 → exact match     (MATCHED)
--   TXN-003 → exact match     (MATCHED)
--   TXN-004 → exact match     (MATCHED)
--   TXN-005 → exact match     (MATCHED)
--   TXN-006 → amount differs  (DISCREPANCY: CSV=1500.50, ledger=1500.00)
--   TXN-007 → no ledger entry (MISSING_IN_LEDGER)
-- =============================================================

INSERT INTO ledger_entries (reference, amount, currency, entry_date, account_code) VALUES
    ('TXN-001', 1500.0000, 'USD', '2024-01-15', 'REV-4001'),
    ('TXN-002',  250.0000, 'USD', '2024-01-15', 'REV-4001'),
    ('TXN-003', 8750.5000, 'USD', '2024-01-15', 'REV-4002'),
    ('TXN-004',   99.9900, 'USD', '2024-01-15', 'REV-4003'),
    ('TXN-005',  320.0000, 'USD', '2024-01-16', 'REV-4001'),
    ('TXN-006', 1500.0000, 'USD', '2024-01-16', 'REV-4001');
-- TXN-007 intentionally has no ledger entry — triggers MISSING_IN_LEDGER

-- Ledger entries for corrections_20240116.csv
-- TXN-008 → MATCHED
-- TXN-009 → DISCREPANCY (ledger has 875.00, CSV has 875.50)
-- TXN-010 → no entry   (MISSING_IN_LEDGER)
-- TXN-011 → bad CSV row, never reaches ledger lookup (SKIPPED)
-- TXN-012 → MATCHED
INSERT INTO ledger_entries (reference, amount, currency, entry_date, account_code) VALUES
    ('TXN-008', 4200.0000, 'USD', '2024-01-16', 'REV-4002'),
    ('TXN-009',  875.0000, 'USD', '2024-01-16', 'REV-4001'),
    ('TXN-012',  300.0000, 'USD', '2024-01-16', 'REV-4001');
-- TXN-010 intentionally has no ledger entry → MISSING_IN_LEDGER
-- TXN-011 never reaches reconciliation → bad CSV row → SKIPPED