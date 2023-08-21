--Referral with approved action Plan
insert into draft_referral (id, intervention_id, created_at, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk)
values ('21814763-4423-4dea-bebc-5c5eb2cb42bf', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-04-22 09:00:00.000000+00', 'X123456', '2500099998', null, null, null, null, null, null, null);

insert into referral (id, intervention_id, created_at, sent_at, sent_by_id, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk, reference_number, supplementary_risk_id)
values ('21814763-4423-4dea-bebc-5c5eb2cb42bf', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-01-11 10:32:12.382884+00', '2021-01-14 15:56:45.382884+00', '2500099998', 'CRN24', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', null, 'AB3892AP',  '5f2debc5-4c6a-4972-84ce-0689b8f9ec52');

insert into referral_details (id, superseded_by_id, referral_id, created_at, created_by, reason_for_change, completion_deadline, further_information, maximum_enforceable_days)
values ('62af5051-10b1-40a6-ba53-070aa2704a51', null, '21814763-4423-4dea-bebc-5c5eb2cb42bf', '2021-01-11 10:32:12.382884+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10);

insert into referral_assignments(referral_id, assigned_at, assigned_by_id, assigned_to_id)
values ('21814763-4423-4dea-bebc-5c5eb2cb42bf', '2021-02-18 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '6c4036b7-e87d-44fb-864f-5a06c1c492f3');

insert into referral_selected_service_category(referral_id, service_category_id)
values ('21814763-4423-4dea-bebc-5c5eb2cb42bf', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_complexity_level_ids(referral_id, complexity_level_ids, complexity_level_ids_key)
values ('21814763-4423-4dea-bebc-5c5eb2cb42bf', 'd0db50b0-4a50-4fc7-a006-9c97530e38b2', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_desired_outcome(referral_id, desired_outcome_id, service_category_id)
values ('21814763-4423-4dea-bebc-5c5eb2cb42bf', '65924ac6-9724-455b-ad30-906936291421', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_service_user_data (referral_id, disabilities, dob, ethnicity, title, first_name, last_name, preferred_language, religion_or_belief, gender)
values ('21814763-4423-4dea-bebc-5c5eb2cb42bf', '{}', TO_DATE('2097-11-08', 'YYYY-MM-DD'), 'White British', 'Sir', 'ANDREW', 'DAVIES', 'Yupik', 'None', 'Male');

insert into supplier_assessment(id, referral_id)
values ('d5718652-fe39-4cc6-b8d5-fd0f57c5b398', '21814763-4423-4dea-bebc-5c5eb2cb42bf');

insert into appointment(id, appointment_time, duration_in_minutes, created_at, created_by_id, referral_id, attended, notifyppof_attendance_behaviour, attendance_failure_information, session_summary, session_response, session_concerns, session_feedback_submitted_by_id, session_feedback_submitted_at, appointment_feedback_submitted_by_id, appointment_feedback_submitted_at, attendance_submitted_by_id, attendance_submitted_at)
values  ('074efba5-ae5e-4e5f-92f4-e10caef9e003', '2021-04-01 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190', 'YES', false, null, 'a good session', 'They responded well', null, '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '2023-01-11 10:32:12.382884+00');

insert into supplier_assessment_appointment(supplier_assessment_id, appointment_id)
values ('d5718652-fe39-4cc6-b8d5-fd0f57c5b398', '074efba5-ae5e-4e5f-92f4-e10caef9e003');

insert into action_plan (id, referral_id, number_of_sessions, created_by_id, created_at, submitted_by_id, submitted_at, approved_at, approved_by_id)
values ('2c3dc03d-9108-415e-887f-73f2a7fb6dc6', '21814763-4423-4dea-bebc-5c5eb2cb42bf', 2, '608955ae-52ed-44cc-884c-011597a77949', '2021-03-10 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', '2021-03-11 17:51:34.235464+00','2021-01-14 15:56:45.382884+00', '2500099998');

insert into action_plan_activity (id, action_plan_id, description, created_at)
values ('0110201d-2ccc-41f7-9746-06bc24560388', '2c3dc03d-9108-415e-887f-73f2a7fb6dc6', 'Identify vacancies and make approach to supported hostel scheme.', '2021-03-11 17:51:34.235464+00');

insert into delivery_session (id, session_number, referral_id)
values ('1f2609f4-b3f7-461f-bcbb-5aca518f0f50', 1, '21814763-4423-4dea-bebc-5c5eb2cb42bf'),
       ('355c813d-182b-433a-bca8-26a30c06c718', 2, '21814763-4423-4dea-bebc-5c5eb2cb42bf');

update draft_referral
set relevant_sentence_id = '1';

update referral
set relevant_sentence_id = '1';
