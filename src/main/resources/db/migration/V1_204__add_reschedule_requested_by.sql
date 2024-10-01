alter table appointment
    add column reschedule_requested_by text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','reschedule_requested_by', TRUE, TRUE);
