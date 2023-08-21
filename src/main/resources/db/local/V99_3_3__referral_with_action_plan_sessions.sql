--Referral with action Plan sessions
insert into draft_referral (id, intervention_id, created_at, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk)
values ('6b381dc1-59fc-46ef-8e74-16daeea11c32', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-04-22 09:00:00.000000+00', 'X123456', '2500099998', null, null, null, null, null, null, null);

insert into referral (id, intervention_id, created_at, sent_at, sent_by_id, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk, reference_number, supplementary_risk_id)
values ('6b381dc1-59fc-46ef-8e74-16daeea11c32', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-01-11 10:32:12.382884+00', '2021-01-14 15:56:45.382884+00', '2500099998', 'CRN24', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', null, 'AB3892SS',  '5f2debc5-4c6a-4972-84ce-0689b8f9ec52');

insert into referral_details (id, superseded_by_id, referral_id, created_at, created_by, reason_for_change, completion_deadline, further_information, maximum_enforceable_days)
values ('90466e24-cce3-4995-85d3-bef8a9543674', null, '6b381dc1-59fc-46ef-8e74-16daeea11c32', '2021-01-11 10:32:12.382884+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10);

insert into referral_assignments(referral_id, assigned_at, assigned_by_id, assigned_to_id)
values ('6b381dc1-59fc-46ef-8e74-16daeea11c32', '2021-02-18 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '6c4036b7-e87d-44fb-864f-5a06c1c492f3');

insert into referral_selected_service_category(referral_id, service_category_id)
values ('6b381dc1-59fc-46ef-8e74-16daeea11c32', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_complexity_level_ids(referral_id, complexity_level_ids, complexity_level_ids_key)
values ('6b381dc1-59fc-46ef-8e74-16daeea11c32', 'd0db50b0-4a50-4fc7-a006-9c97530e38b2', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_desired_outcome(referral_id, desired_outcome_id, service_category_id)
values ('6b381dc1-59fc-46ef-8e74-16daeea11c32', '65924ac6-9724-455b-ad30-906936291421', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_service_user_data (referral_id, disabilities, dob, ethnicity, title, first_name, last_name, preferred_language, religion_or_belief, gender)
values ('6b381dc1-59fc-46ef-8e74-16daeea11c32', '{}', TO_DATE('2097-11-08', 'YYYY-MM-DD'), 'White British', 'Sir', 'ANDREW', 'DAVIES', 'Yupik', 'None', 'Male');

insert into supplier_assessment(id, referral_id)
values ('127cdec1-8fd2-4a2b-847e-ab776f739681', '6b381dc1-59fc-46ef-8e74-16daeea11c32');

insert into appointment(id, appointment_time, duration_in_minutes, created_at, created_by_id, referral_id, attended, notifyppof_attendance_behaviour, attendance_failure_information, session_summary, session_response, session_concerns, session_feedback_submitted_by_id, session_feedback_submitted_at, appointment_feedback_submitted_by_id, appointment_feedback_submitted_at, attendance_submitted_by_id, attendance_submitted_at)
values  ('fce72d40-c0df-46dc-bfd5-bee34922e55e', '2021-04-01 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190', 'YES', false, null, 'a good session', 'They responded well', null, '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00');

insert into supplier_assessment_appointment(supplier_assessment_id, appointment_id)
values ('127cdec1-8fd2-4a2b-847e-ab776f739681', 'fce72d40-c0df-46dc-bfd5-bee34922e55e');

insert into action_plan (id, referral_id, number_of_sessions, created_by_id, created_at, submitted_by_id, submitted_at, approved_at, approved_by_id)
values ('923d27ff-520f-4e96-88d4-a3ff5fdd8736', '6b381dc1-59fc-46ef-8e74-16daeea11c32', 2, '608955ae-52ed-44cc-884c-011597a77949', '2021-03-10 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', '2021-03-11 17:51:34.235464+00','2021-01-14 15:56:45.382884+00', '2500099998');

insert into action_plan_activity (id, action_plan_id, description, created_at)
values ('cd4a87fe-9900-4f95-8645-f6d4695a8d0e', '923d27ff-520f-4e96-88d4-a3ff5fdd8736', 'Identify vacancies and make approach to supported hostel scheme.', '2021-03-11 17:51:34.235464+00');

insert into delivery_session (id, session_number, referral_id)
values ('d6651490-0975-4237-874d-9c03fd1df6a4', 1, '6b381dc1-59fc-46ef-8e74-16daeea11c32'),
       ('5249bf43-c317-4650-8b1f-f8d24f8a3e0d', 2, '6b381dc1-59fc-46ef-8e74-16daeea11c32');

insert into appointment(id, appointment_time, duration_in_minutes, created_at, created_by_id, referral_id, attended, notifyppof_attendance_behaviour, attendance_failure_information, session_summary, session_response, session_concerns, session_feedback_submitted_by_id, session_feedback_submitted_at, appointment_feedback_submitted_by_id, appointment_feedback_submitted_at, attendance_submitted_by_id, attendance_submitted_at)
values  ('219b9b73-ea25-4e37-b79a-a357cfe6cb95', '2021-04-01 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190', 'YES', false, null, 'a good session', 'They responded well', null, '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00'),
        ('dba6b4fd-b6ee-4965-8014-0a6edd32639e', '2021-04-02 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190', 'YES', false, null, 'a good session', 'They responded well', null, '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00');


insert into delivery_session_appointment (delivery_session_id, appointment_id)
values ('d6651490-0975-4237-874d-9c03fd1df6a4', '219b9b73-ea25-4e37-b79a-a357cfe6cb95'),
       ('5249bf43-c317-4650-8b1f-f8d24f8a3e0d', 'dba6b4fd-b6ee-4965-8014-0a6edd32639e');

update draft_referral
set relevant_sentence_id = '1';

update referral
set relevant_sentence_id = '1';
