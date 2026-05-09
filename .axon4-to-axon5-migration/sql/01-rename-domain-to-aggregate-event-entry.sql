-- AF4 → AF5 schema migration: rename domain_event_entry → aggregate_event_entry
-- Target: PostgreSQL (project uses PostgreSQL via spring-boot-starter-data-jpa)
--
-- Run BEFORE deploying the AF5 build. Operates on the rows already present
-- in the AF4 domain_event_entry table — same DB, same payload bytes, no
-- export/import. Wrap in a transaction. Verify row counts before and after.
--
-- NOTE: Hibernate ddl-auto: update is enabled in application.yaml. If the
-- AF5 app starts before this script runs, Hibernate will auto-create an
-- empty aggregate_event_entry alongside the AF4 domain_event_entry —
-- migration becomes harder. Apply this script first, then start AF5.

BEGIN;

-- 1. Rename the table.
ALTER TABLE domain_event_entry RENAME TO aggregate_event_entry;

-- 2. Rename columns to AF5 names.
ALTER TABLE aggregate_event_entry RENAME COLUMN event_identifier     TO identifier;
ALTER TABLE aggregate_event_entry RENAME COLUMN payload_type         TO type;
ALTER TABLE aggregate_event_entry RENAME COLUMN payload_revision     TO version;
ALTER TABLE aggregate_event_entry RENAME COLUMN time_stamp           TO timestamp;
ALTER TABLE aggregate_event_entry RENAME COLUMN type                 TO aggregate_type;
ALTER TABLE aggregate_event_entry RENAME COLUMN sequence_number      TO aggregate_sequence_number;
ALTER TABLE aggregate_event_entry RENAME COLUMN meta_data            TO metadata;

-- 3. Tighten constraints (AF5 stricter than AF4).
--    AF4 allowed null payload_revision; AF5 requires it.
UPDATE aggregate_event_entry SET version = '0' WHERE version IS NULL;
ALTER TABLE aggregate_event_entry ALTER COLUMN version SET NOT NULL;

--    aggregate_sequence_number is now NOT NULL.
ALTER TABLE aggregate_event_entry ALTER COLUMN aggregate_sequence_number SET NOT NULL;

--    identifier is now NOT NULL.
ALTER TABLE aggregate_event_entry ALTER COLUMN identifier SET NOT NULL;

--    aggregate_identifier is now optional (DCB-friendly). PostgreSQL columns
--    are nullable by default, so explicitly drop NOT NULL if it was set.
ALTER TABLE aggregate_event_entry ALTER COLUMN aggregate_identifier DROP NOT NULL;

-- 4. Drop AF4 length cap on payload / metadata. PostgreSQL stores both as
--    bytea (no length cap), so nothing to do — left here for portability.
-- ALTER TABLE aggregate_event_entry ALTER COLUMN payload  TYPE bytea;
-- ALTER TABLE aggregate_event_entry ALTER COLUMN metadata TYPE bytea;

-- 5. Sequence generator with allocation size 1 (AF5 default).
CREATE SEQUENCE IF NOT EXISTS "aggregate-event-global-index-sequence"
    INCREMENT BY 1 MINVALUE 1;

--    Seed past the highest existing global_index so new inserts don't collide.
SELECT setval(
    '"aggregate-event-global-index-sequence"',
    GREATEST(COALESCE((SELECT MAX(global_index) FROM aggregate_event_entry), 0), 1)
);

-- 6. Unique index on (aggregate_identifier, aggregate_sequence_number).
CREATE UNIQUE INDEX IF NOT EXISTS aggregate_event_entry_aggidx
    ON aggregate_event_entry (aggregate_identifier, aggregate_sequence_number);

COMMIT;

-- Verification queries (run separately, NOT inside the transaction):
--
--   SELECT COUNT(*) FROM aggregate_event_entry;
--   SELECT MAX(global_index) FROM aggregate_event_entry;
--   SELECT last_value FROM "aggregate-event-global-index-sequence";
--
-- Row count must equal the pre-migration count of domain_event_entry.
-- Sequence last_value must be >= MAX(global_index).
