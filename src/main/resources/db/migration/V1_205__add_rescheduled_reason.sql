alter table appointment
    add column rescheduled_reason text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','rescheduled_reason', TRUE, TRUE);
