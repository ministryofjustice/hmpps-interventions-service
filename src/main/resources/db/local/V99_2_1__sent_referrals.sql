insert into draft_referral (id, intervention_id, created_at, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk)
values ('81d754aa-d868-4347-9c0f-50690773014e', '98a42c61-c30f-4beb-8062-04033c376e2d', '2021-01-11 10:32:12.382884+00', 'CRN23', '2500099998', null, null, null, null, null, null, null),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', '3ccb511b-89b2-42f7-803b-304f54d85a24', '2021-01-11 10:32:12.382884+00', 'CRN23', '2500099998', null, null, null, null, null, null, null);

insert into referral (id, intervention_id, created_at, sent_at, sent_by_id, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk, reference_number, supplementary_risk_id)
values ('81d754aa-d868-4347-9c0f-50690773014e', '98a42c61-c30f-4beb-8062-04033c376e2d', '2021-01-11 10:32:12.382884+00', '2021-01-14 15:56:45.382884+00', '2500099998', 'CRN23', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', null, 'HD2123AC', '7cfc77d5-ea79-4201-bc8b-ac35b4f4d4a3'),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', '3ccb511b-89b2-42f7-803b-304f54d85a24', '2021-01-11 10:32:12.382884+00', '2021-01-14 15:56:45.382884+00', '2500099998', 'CRN23', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', null, 'AJ1827DR', 'a6586f6a-1be9-44ef-9a7c-685dbcff9769');

insert into referral_details (id, superseded_by_id, referral_id, created_at, created_by, reason_for_change, completion_deadline, further_information, maximum_enforceable_days)
values ('e8ec245d-156a-4fc9-a4c9-7bd918095398', null, '81d754aa-d868-4347-9c0f-50690773014e', '2021-01-11 10:32:12.382884+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10),
       ('f050bde8-69c7-4e6a-bbe2-7092292667aa', null, 'f89bd739-b9a2-482e-9947-12a793abcfb1', '2021-01-11 10:32:12.382884+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10);

insert into referral_selected_service_category(referral_id, service_category_id)
values ('81d754aa-d868-4347-9c0f-50690773014e', '428ee70f-3001-4399-95a6-ad25eaaede16'),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', 'b84f4eb7-4db0-477e-8c59-21027b3262c5'),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', 'c036826e-f077-49a5-8b33-601dca7ad479');

insert into referral_complexity_level_ids(referral_id, complexity_level_ids, complexity_level_ids_key)
values ('81d754aa-d868-4347-9c0f-50690773014e', 'd0db50b0-4a50-4fc7-a006-9c97530e38b2', '428ee70f-3001-4399-95a6-ad25eaaede16'),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', '707a0244-6bfe-4f0a-80d9-5e79498e81a8', 'b84f4eb7-4db0-477e-8c59-21027b3262c5');

insert into referral_service_user_data (referral_id, disabilities, dob, ethnicity, title, first_name, last_name, preferred_language, religion_or_belief, gender)
values ('81d754aa-d868-4347-9c0f-50690773014e', '{Autism spectrum condition, sciatica}', TO_DATE('2097-11-08', 'YYYY-MM-DD'), 'British', 'Mrs', 'STEVE', 'MCDONALD', 'English', 'Agnostic', 'Female'),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', '{Autism spectrum condition, sciatica}', TO_DATE('2097-11-08', 'YYYY-MM-DD'), 'British', 'Mrs', 'STEVE', 'MCDONALD', 'English', 'Agnostic', 'Female');

insert into referral_desired_outcome (referral_id, desired_outcome_id, service_category_id)
values ('81d754aa-d868-4347-9c0f-50690773014e', '301ead30-30a4-4c7c-8296-2768abfb59b5', '428ee70f-3001-4399-95a6-ad25eaaede16'),
       ('81d754aa-d868-4347-9c0f-50690773014e', '65924ac6-9724-455b-ad30-906936291421', '428ee70f-3001-4399-95a6-ad25eaaede16'),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', 'f2aa2f28-27b5-4e79-856e-6a3f19550b20', 'b84f4eb7-4db0-477e-8c59-21027b3262c5'),
       ('f89bd739-b9a2-482e-9947-12a793abcfb1', 'b820cdf3-164b-427d-b78e-1fb079851ebd', 'c036826e-f077-49a5-8b33-601dca7ad479');

insert into supplier_assessment(id, referral_id)
values ('fbc4a21f-eb15-489d-b16f-b3b82b008722', '81d754aa-d868-4347-9c0f-50690773014e'),
       ('1db491c4-ee7d-4fa7-b42f-7b1aa81039f0', 'f89bd739-b9a2-482e-9947-12a793abcfb1');

insert into appointment(id, appointment_time, duration_in_minutes, created_at, created_by_id, referral_id)
values  ('71f0de69-baba-4f29-8f59-62af4bb4c63d', '2021-04-01 12:00:00.000000+00', 120, '2021-03-12 17:51:34.235464+00', '608955ae-52ed-44cc-884c-011597a77949', '81d754aa-d868-4347-9c0f-50690773014e');

insert into supplier_assessment_appointment(supplier_assessment_id, appointment_id)
values ('fbc4a21f-eb15-489d-b16f-b3b82b008722', '71f0de69-baba-4f29-8f59-62af4bb4c63d');

