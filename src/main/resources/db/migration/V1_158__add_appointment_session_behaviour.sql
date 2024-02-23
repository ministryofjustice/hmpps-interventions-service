alter table appointment
    add column notify_probation_practitioner_of_behaviour boolean,
    add column notify_probation_practitioner_of_concerns boolean,
    add column session_behaviour text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','notify_probation_practitioner_of_behaviour', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','notify_probation_practitioner_of_concerns', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','session_behaviour', TRUE, TRUE);