ALTER TABLE desired_outcome ADD COLUMN deprecated_at timestamp with time zone null;

COMMENT ON COLUMN desired_outcome.deprecated_at IS 'This column shows whether an outcome has been deprecated or not. Non-deprecated outcomes will be null';

INSERT INTO metadata(table_name, column_name, sensitive, domain_data) VALUES ('desired_outcome', 'deprecated_at',false, true);