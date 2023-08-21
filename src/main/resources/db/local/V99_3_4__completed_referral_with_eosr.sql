--Referral with eosr
insert into draft_referral (id, intervention_id, created_at, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk)
values ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-04-22 09:00:00.000000+00', 'X123456', '2500099998', null, null, null, null, null, null, null);

insert into referral (id, intervention_id, created_at, sent_at, sent_by_id, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk, reference_number, supplementary_risk_id, concluded_at)
values ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-01-11 10:32:12.382884+00', '2021-01-14 15:56:45.382884+00', '2500099998', 'CRN24', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', null, 'AB3892EO',  '5f2debc5-4c6a-4972-84ce-0689b8f9ec52', '2021-01-14 15:56:45.382884+00');

insert into referral_details (id, superseded_by_id, referral_id, created_at, created_by, reason_for_change, completion_deadline, further_information, maximum_enforceable_days)
values ('2f1e6b73-e9d1-4bce-84a7-b9f73b301842', null, 'dba6b4fd-b6ee-4965-8014-0a6edd32639e', '2021-01-11 10:32:12.382884+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10);

insert into referral_assignments(referral_id, assigned_at, assigned_by_id, assigned_to_id)
values ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', '2021-02-18 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '6c4036b7-e87d-44fb-864f-5a06c1c492f3');

insert into referral_selected_service_category(referral_id, service_category_id)
values ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_complexity_level_ids(referral_id, complexity_level_ids, complexity_level_ids_key)
values ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', 'd0db50b0-4a50-4fc7-a006-9c97530e38b2', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_desired_outcome(referral_id, desired_outcome_id, service_category_id)
values ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', '65924ac6-9724-455b-ad30-906936291421', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_service_user_data (referral_id, disabilities, dob, ethnicity, title, first_name, last_name, preferred_language, religion_or_belief, gender)
values ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', '{}', TO_DATE('2097-11-08', 'YYYY-MM-DD'), 'White British', 'Sir', 'ANDREW', 'DAVIES', 'Yupik', 'None', 'Male');

insert into supplier_assessment(id, referral_id)
values ('7ab5866a-d325-4f49-85fe-8d7b47197038', 'dba6b4fd-b6ee-4965-8014-0a6edd32639e');

insert into appointment(id, appointment_time, duration_in_minutes, created_at, created_by_id, referral_id, attended, notifyppof_attendance_behaviour, attendance_failure_information, session_summary, session_response, session_concerns, session_feedback_submitted_by_id, session_feedback_submitted_at, appointment_feedback_submitted_by_id, appointment_feedback_submitted_at, attendance_submitted_by_id, attendance_submitted_at)
values  ('d0d96fbd-3392-4848-9cda-0a37ffdf81e0', '2021-04-01 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190', 'YES', false, null, 'a good session', 'They responded well', null, '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00');

insert into supplier_assessment_appointment(supplier_assessment_id, appointment_id)
values ('7ab5866a-d325-4f49-85fe-8d7b47197038', 'd0d96fbd-3392-4848-9cda-0a37ffdf81e0');

insert into action_plan (id, referral_id, number_of_sessions, created_by_id, created_at, submitted_by_id, submitted_at, approved_at, approved_by_id)
values ('f28e6a9d-40ee-412d-aa20-d05ed1f4a57d', 'dba6b4fd-b6ee-4965-8014-0a6edd32639e', 2, '608955ae-52ed-44cc-884c-011597a77949', '2021-03-10 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', '2021-03-11 17:51:34.235464+00','2021-01-14 15:56:45.382884+00', '2500099998');

insert into action_plan_activity (id, action_plan_id, description, created_at)
values ('36d3eddb-e345-4c65-9b06-fd1dc56ea155', 'f28e6a9d-40ee-412d-aa20-d05ed1f4a57d', 'Identify vacancies and make approach to supported hostel scheme.', '2021-03-11 17:51:34.235464+00');

insert into delivery_session (id, session_number, referral_id)
values ('127605f4-3c33-4386-8098-7fc710218c56', 1, 'dba6b4fd-b6ee-4965-8014-0a6edd32639e'),
       ('c18f0ad9-d2c1-4823-888c-f201e2ab5716', 2, 'dba6b4fd-b6ee-4965-8014-0a6edd32639e');

insert into appointment(id, appointment_time, duration_in_minutes, created_at, created_by_id, referral_id, attended, notifyppof_attendance_behaviour, attendance_failure_information, session_summary, session_response, session_concerns, session_feedback_submitted_by_id, session_feedback_submitted_at, appointment_feedback_submitted_by_id, appointment_feedback_submitted_at, attendance_submitted_by_id, attendance_submitted_at)
values  ('32f272c5-5b75-4b4e-bdb3-fabc5da9ea63', '2021-04-01 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190', 'YES', false, null, 'a good session', 'They responded well', null, '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00'),
        ('154b8fc8-0231-48e3-aa3d-5fda29eac1e5', '2021-04-02 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190', 'YES', false, null, 'a good session', 'They responded well', null, '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00');

insert into delivery_session_appointment (delivery_session_id, appointment_id)
values ('127605f4-3c33-4386-8098-7fc710218c56', '32f272c5-5b75-4b4e-bdb3-fabc5da9ea63'),
       ('c18f0ad9-d2c1-4823-888c-f201e2ab5716', '154b8fc8-0231-48e3-aa3d-5fda29eac1e5');

insert into end_of_service_report(id, referral_id, created_at, created_by_id, submitted_by_id, submitted_at, further_information)
values ('7e897f77-fb09-479b-8eb9-d6d729e4c8e7', 'dba6b4fd-b6ee-4965-8014-0a6edd32639e', '2021-04-22 09:00:00.000000+00', '608955ae-52ed-44cc-884c-011597a77949', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', 'some random information');

insert into end_of_service_report_outcome(end_of_service_report_id, desired_outcome_id, achievement_level, progression_comments, additional_task_comments)
values('7e897f77-fb09-479b-8eb9-d6d729e4c8e7', '65924ac6-9724-455b-ad30-906936291421', 'ACHIEVED', 'some progression comments', 'some additional task comments');

update draft_referral
set relevant_sentence_id = '1';

update referral
set relevant_sentence_id = '1';
