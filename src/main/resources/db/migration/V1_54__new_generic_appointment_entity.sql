create table appointment(
    id uuid not null,
    appointment_time timestamp with time zone,
    duration_in_minutes int,
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

create table action_plan_session (
    id uuid not null,
    session_number int not null,
    action_plan_id uuid,
    constraint fk_action_plan_session_action_plan foreign key (action_plan_id) references action_plan,
    primary key (id)
);

create table action_plan_session_appointments (
    action_plan_session_id uuid not null,
    appointments_id uuid not null,
    constraint fk_action_plan_session_appointments_action_plan_session foreign key (action_plan_session_id) references action_plan_session,
    constraint fk_action_plan_session_appointments_appointment_id foreign key (appointments_id) references appointment,
    constraint uk_action_plan_session_appointments_appointments_id unique (appointments_id),
    primary key (action_plan_session_id, appointments_id)
);

insert into action_plan_session
    select id, session_number, action_plan_id from action_plan_appointment;

drop table action_plan_appointment;
