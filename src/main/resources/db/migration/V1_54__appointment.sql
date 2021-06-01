-- CREATE TYPE attended AS ENUM ('YES','LATE', 'NO');

create table appointment(
    id uuid not null,
    appointment_time timestamp with time zone null,
    duration_in_minutes int null,
    created_at timestamp with time zone not null,
    created_by_id text not null,

    attended attended,
    additional_attendance_information text,
    attendance_submitted_at timestamp with time zone,

    attendance_behaviour text,
    notifyppof_attendance_behaviour boolean,
    attendance_behaviour_submitted_at timestamp with time zone,

    session_feedback_submitted_at timestamp with time zone,
    delius_appointment_id bigint,

    constraint pk_appt_id primary key (id),
    constraint fk_appt_to_created_by foreign key (created_by_id) references auth_user
);

alter table action_plan_appointment
    drop column appointment_time,
    drop column duration_in_minutes,
    drop column created_at,
    drop column created_by_id,

    drop column attended,
    drop column additional_attendance_information,
    drop column attendance_submitted_at,

    drop column attendance_behaviour,
    drop column notifyppof_attendance_behaviour,
    drop column attendance_behaviour_submitted_at,

    drop column session_feedback_submitted_at,
    drop column delius_appointment_id;

alter table action_plan_appointment
    add column appointment_id uuid,
    add constraint fk_action_plan_app_id foreign key (appointment_id) references appointment;

alter table action_plan_appointment
rename to action_plan_session
