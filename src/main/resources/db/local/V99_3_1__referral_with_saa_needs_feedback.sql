--Referral with saa needing feedback
insert into draft_referral (id, intervention_id, created_at, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk)
values ('61cc8eeb-112e-4c60-8706-d0f95afdd6a3', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-04-22 09:00:00.000000+00', 'X123456', '2500099998', null, null, null, null, null, null, null);

insert into referral (id, intervention_id, created_at, sent_at, sent_by_id, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk, reference_number, supplementary_risk_id)
values ('61cc8eeb-112e-4c60-8706-d0f95afdd6a3', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-01-11 10:32:12.382884+00', '2021-01-14 15:56:45.382884+00', '2500099998', 'CRN24', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', null, 'AB3892SA',  '5f2debc5-4c6a-4972-84ce-0689b8f9ec52');

insert into referral_details (id, superseded_by_id, referral_id, created_at, created_by, reason_for_change, completion_deadline, further_information, maximum_enforceable_days)
values ('da257b7b-21d7-4301-8ee2-f708c29c7469', null, '61cc8eeb-112e-4c60-8706-d0f95afdd6a3', '2021-01-11 10:32:12.382884+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10);

insert into referral_assignments(referral_id, assigned_at, assigned_by_id, assigned_to_id)
values ('61cc8eeb-112e-4c60-8706-d0f95afdd6a3', '2021-02-18 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '6c4036b7-e87d-44fb-864f-5a06c1c492f3');

insert into referral_selected_service_category(referral_id, service_category_id)
values ('61cc8eeb-112e-4c60-8706-d0f95afdd6a3', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_complexity_level_ids(referral_id, complexity_level_ids, complexity_level_ids_key)
values ('61cc8eeb-112e-4c60-8706-d0f95afdd6a3', 'd0db50b0-4a50-4fc7-a006-9c97530e38b2', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_desired_outcome(referral_id, desired_outcome_id, service_category_id)
values ('61cc8eeb-112e-4c60-8706-d0f95afdd6a3', '65924ac6-9724-455b-ad30-906936291421', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_service_user_data (referral_id, disabilities, dob, ethnicity, title, first_name, last_name, preferred_language, religion_or_belief, gender)
values ('61cc8eeb-112e-4c60-8706-d0f95afdd6a3', '{}', TO_DATE('2097-11-08', 'YYYY-MM-DD'), 'White British', 'Sir', 'ANDREW', 'DAVIES', 'Yupik', 'None', 'Male');

insert into supplier_assessment(id, referral_id)
values ('3cb1c241-f193-425d-8539-9e67773f0b8c', '61cc8eeb-112e-4c60-8706-d0f95afdd6a3');

insert into appointment(id, appointment_time, duration_in_minutes, created_at, created_by_id, referral_id)
values  ('11910753-7522-4621-9d8f-aad1660171e8', '2021-04-01 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', 'b59d3599-0681-466a-82b2-f6f957e46190');

insert into supplier_assessment_appointment(supplier_assessment_id, appointment_id)
values ('3cb1c241-f193-425d-8539-9e67773f0b8c', '11910753-7522-4621-9d8f-aad1660171e8');

update draft_referral
set relevant_sentence_id = '1';

update referral
set relevant_sentence_id = '1';
