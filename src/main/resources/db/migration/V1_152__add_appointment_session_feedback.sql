CREATE TYPE no_session_reason_type AS ENUM ('POP_ACCEPTABLE', 'POP_UNACCEPTABLE', 'LOGISTICS');

alter table appointment
    add column did_session_happen boolean,
    add column no_attendance_information text,
    add column late boolean,
    add column late_reason text,
    add column future_session_plans text,
    add column no_session_reason_type no_session_reason_type,
    add column no_session_reason_pop_acceptable text,
    add column no_session_reason_pop_unacceptable text,
    add column no_session_reason_logistics text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','did_session_happen', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','no_attendance_information', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','late', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','late_reason', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','future_session_plans', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','no_session_reason_type', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','no_session_reason_pop_acceptable', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','no_session_reason_pop_unacceptable', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','no_session_reason_logistics', TRUE, TRUE);