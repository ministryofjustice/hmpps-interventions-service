alter table appointment
    add column superseded_by_appointment_id uuid NULL,
    add column stale bool NOT NULL DEFAULT false;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','superseded_by_appointment_id', FALSE, FALSE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','stale', FALSE, FALSE);