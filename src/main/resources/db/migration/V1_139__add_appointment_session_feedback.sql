alter table appointment
    add column attendance_failure_information text,
    add column session_summary text,
    add column session_response text,
    add column session_concerns text,
    add column session_feedback_submitted_by_id text,
    add column session_feedback_submitted_at timestamp with time zone,

    add constraint fk_session_feedback_submitted_by_id foreign key (session_feedback_submitted_by_id) references auth_user;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','attendance_failure_information', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','session_summary', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','session_response', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','session_concerns', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','session_feedback_submitted_by_id', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('appointment','session_feedback_submitted_at', FALSE, TRUE);