--New empty referral
insert into draft_referral (id, intervention_id, created_at, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk)
values ('f73df1de-6aaf-4678-920f-0f0270bcd885', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-04-22 09:00:00.000000+00', 'X123456', '2500099998', null, null, null, null, null, null, null);

insert into referral (id, intervention_id, created_at, sent_at, sent_by_id, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk, reference_number, supplementary_risk_id)
values ('f73df1de-6aaf-4678-920f-0f0270bcd885', '08524319-7d5b-4b56-862a-bfe2c9a545f5', '2021-01-11 10:32:12.382884+00', '2021-01-14 15:56:45.382884+00', '2500099998', 'CRN24', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', null, 'AB3892AC',  '5f2debc5-4c6a-4972-84ce-0689b8f9ec52');

insert into referral_details (id, superseded_by_id, referral_id, created_at, created_by, reason_for_change, completion_deadline, further_information, maximum_enforceable_days)
values ('bcb09584-e2df-41bd-837e-f3cebaf77bf4', null, 'f73df1de-6aaf-4678-920f-0f0270bcd885', '2021-01-11 10:32:12.382884+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10);

insert into referral_assignments(referral_id, assigned_at, assigned_by_id, assigned_to_id)
values ('f73df1de-6aaf-4678-920f-0f0270bcd885', '2021-02-18 10:32:12.382884+00', '6c4036b7-e87d-44fb-864f-5a06c1c492f3', '6c4036b7-e87d-44fb-864f-5a06c1c492f3');

insert into referral_selected_service_category(referral_id, service_category_id)
values ('f73df1de-6aaf-4678-920f-0f0270bcd885', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_complexity_level_ids(referral_id, complexity_level_ids, complexity_level_ids_key)
values ('f73df1de-6aaf-4678-920f-0f0270bcd885', 'd0db50b0-4a50-4fc7-a006-9c97530e38b2', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_desired_outcome(referral_id, desired_outcome_id, service_category_id)
values ('f73df1de-6aaf-4678-920f-0f0270bcd885', '65924ac6-9724-455b-ad30-906936291421', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_service_user_data (referral_id, disabilities, dob, ethnicity, title, first_name, last_name, preferred_language, religion_or_belief, gender)
values ('f73df1de-6aaf-4678-920f-0f0270bcd885', '{}', TO_DATE('2097-11-08', 'YYYY-MM-DD'), 'White British', 'Sir', 'ANDREW', 'DAVIES', 'Yupik', 'None', 'Male');

insert into supplier_assessment(id, referral_id)
values ('6a94e2aa-e3c2-431f-9b70-a4f2d90c1e6b', 'f73df1de-6aaf-4678-920f-0f0270bcd885');

update draft_referral
set relevant_sentence_id = '1';

update referral
set relevant_sentence_id = '1';
