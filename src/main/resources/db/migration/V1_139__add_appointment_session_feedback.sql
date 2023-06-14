alter table appointment
    add column attendance_failure_information text,
    add column session_summary text,
    add column session_response text,
    add column session_concerns text,
    add column session_feedback_submitted_by_id text,
    add column session_feedback_submitted_at timestamp with time zone,

    add constraint fk_session_feedback_submitted_by_id foreign key (session_feedback_submitted_by_id) references auth_user;