-- to enable querying the single "live" appointments
ALTER TABLE appointment
    ADD COLUMN superseded bool NOT NULL DEFAULT false;

-- migrating the values to "true" will be done in when a new reschedule start to happen
-- • all the appointments can be false to start with
-- • reschedule appointments happens, new appointments will be created instead of overwriting and the old appointments will be set to true

-- inserting the metadata for the new column
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','superseded',FALSE, TRUE);