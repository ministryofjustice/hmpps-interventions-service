insert into referral (id, intervention_id, created_at, sent_at, sent_by_id, service_usercrn, created_by_id, additional_needs_information, accessibility_needs, needs_interpreter, interpreter_language, has_additional_responsibilities, when_unavailable, draft_supplementary_risk, reference_number)
values ('ac386c25-52c8-41fa-9213-fcf42e24b0b5', '98a42c61-c30f-4beb-8062-04033c376e2d', '2020-12-07 18:02:01.599803+00', null, null, 'CRN16', '2500099998', null, null, null, null, null, null, null, null),
       ('dfb64747-f658-40e0-a827-87b4b0bdcfed', '98a42c61-c30f-4beb-8062-04033c376e2d', '2020-12-07 20:45:21.986389+00', null, null, 'CRN17', '11', null, null, null, null, null, null, null, null),
       ('745011c1-1ae5-45e4-99cb-6e1f2f8ccab9', '98a42c61-c30f-4beb-8062-04033c376e2d', '2020-12-07 20:45:21.986389+00', null, null, 'CRN18', '2500099998', null, null, null, null, null, null, null, null),
       ('d496e4a7-7cc1-44ea-ba67-c295084f1962', '98a42c61-c30f-4beb-8062-04033c376e2d', '2020-12-24 09:32:32.871623+00', null, null, 'CRN19', '2500099998',  null, null, null, null, null, null, null, null),
       ('1219a064-709b-4b6c-a11e-10b8cb3966f6', '98a42c61-c30f-4beb-8062-04033c376e2d', '2021-01-12 14:46:21.987234+00', null, null, 'CRN20', '2500099998', null, null, null, null, null, null, null, null),
       ('037cc90b-beaa-4a32-9ab7-7f79136e1d27', '98a42c61-c30f-4beb-8062-04033c376e2d', '2021-01-12 14:46:21.987234+00', null, null, 'CRN21', '2500099998', null, null, null, null, null, null, null, null),
       ('2a67075a-9c77-4103-9de0-63c4cfe3e8d6', '98a42c61-c30f-4beb-8062-04033c376e2d', '2021-01-12 14:46:21.987234+00', null, null, 'CRN22', '2500099998', 'Alex is currently sleeping on her aunt''s sofa', 'She uses a wheelchair', true, 'Spanish', true, 'She works Mondays 9am - midday', 'A danger to the elderly', null);

insert into referral_details (id, superseded_by_id, referral_id, created_at, created_by, reason_for_change, completion_deadline, further_information, maximum_enforceable_days)
values ('6e9a311f-2369-409e-9286-6e9e0569a180', null, 'ac386c25-52c8-41fa-9213-fcf42e24b0b5', '2020-12-07 18:02:01.599803+00', '2500099998', 'initial referral details', '2021-02-14', null, null),
       ('d5d858b2-6cfc-4c76-bcb2-df7edcb4caa2', null, 'dfb64747-f658-40e0-a827-87b4b0bdcfed', '2020-12-07 20:45:21.986389+00', '2500099998', 'initial referral details', '2021-03-01', null, null),
       ('2f997476-dd55-4189-b322-abc48bab8e38', null, '745011c1-1ae5-45e4-99cb-6e1f2f8ccab9', '2020-12-07 20:45:21.986389+00', '2500099998', 'initial referral details', null, null, null),
       ('a76347dc-dab1-48cf-a2b7-89a7c56a706b', null, 'd496e4a7-7cc1-44ea-ba67-c295084f1962', '2020-12-24 09:32:32.871623+00', '2500099998', 'initial referral details', '2021-01-30', null, null),
       ('69b55d8e-0cde-469d-a35a-742be9d2d246', null, '1219a064-709b-4b6c-a11e-10b8cb3966f6', '2021-01-12 14:46:21.987234+00', '2500099998', 'initial referral details', null, null, null),
       ('0698819d-0262-4786-b8fe-58d6553b90bb', null, '037cc90b-beaa-4a32-9ab7-7f79136e1d27', '2021-01-12 14:46:21.987234+00', '2500099998', 'initial referral details', null, null, null),
       ('64eac6d2-4b49-473c-bc8c-a7e620ee2bf6', null, '2a67075a-9c77-4103-9de0-63c4cfe3e8d6', '2021-01-12 14:46:21.987234+00', '2500099998', 'initial referral details', '2021-04-01', 'Some information about the service user', 10);

insert into referral_selected_service_category(referral_id, service_category_id)
values ('ac386c25-52c8-41fa-9213-fcf42e24b0b5', '428ee70f-3001-4399-95a6-ad25eaaede16'),
        ('dfb64747-f658-40e0-a827-87b4b0bdcfed', '428ee70f-3001-4399-95a6-ad25eaaede16'),
        ('745011c1-1ae5-45e4-99cb-6e1f2f8ccab9', '428ee70f-3001-4399-95a6-ad25eaaede16'),
        ('d496e4a7-7cc1-44ea-ba67-c295084f1962', '428ee70f-3001-4399-95a6-ad25eaaede16'),
        ('1219a064-709b-4b6c-a11e-10b8cb3966f6', '428ee70f-3001-4399-95a6-ad25eaaede16'),
        ('037cc90b-beaa-4a32-9ab7-7f79136e1d27', '428ee70f-3001-4399-95a6-ad25eaaede16'),
        ('2a67075a-9c77-4103-9de0-63c4cfe3e8d6', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_complexity_level_ids(referral_id, complexity_level_ids, complexity_level_ids_key)
values ('2a67075a-9c77-4103-9de0-63c4cfe3e8d6', 'd0db50b0-4a50-4fc7-a006-9c97530e38b2', '428ee70f-3001-4399-95a6-ad25eaaede16');

insert into referral_service_user_data (referral_id, disabilities, dob, ethnicity, title, first_name, last_name, preferred_language, religion_or_belief, gender)
values ('ac386c25-52c8-41fa-9213-fcf42e24b0b5', '{Autism spectrum condition}', TO_DATE('1980-08-10', 'YYYY-MM-DD'), 'White British', 'Accepted', 'BEN', 'CASSIDY', 'English', 'None', 'Male'),
       ('dfb64747-f658-40e0-a827-87b4b0bdcfed', '{}', TO_DATE('1980-08-10', 'YYYY-MM-DD'), 'White British', 'Ms', 'DULQRENIERRE', 'LENOLD', 'Gujarati', 'None', 'Female'),
       ('745011c1-1ae5-45e4-99cb-6e1f2f8ccab9', '{}', TO_DATE('1980-08-10', 'YYYY-MM-DD'), 'White British', 'Ms', 'ULVAMOSE', 'JAYMENTINO', 'Welsh', 'None', 'Female'),
       ('d496e4a7-7cc1-44ea-ba67-c295084f1962', '{}', TO_DATE('1980-08-10', 'YYYY-MM-DD'), 'White British', 'Mr', 'OGANONTHOMASIN', 'DEVIEVE', 'English', 'None', 'Male'),
       ('1219a064-709b-4b6c-a11e-10b8cb3966f6', '{}', TO_DATE('1980-08-10', 'YYYY-MM-DD'), 'White British', 'Mr', 'YMYNNEUMAR', 'ZACHASINA', 'English', 'None', 'Male'),
       ('037cc90b-beaa-4a32-9ab7-7f79136e1d27', '{}', TO_DATE('1980-08-10', 'YYYY-MM-DD'), 'White British', 'Ms', 'ILBNELIWA', 'ANOLPH', 'Spanish', 'None', 'Female'),
       ('2a67075a-9c77-4103-9de0-63c4cfe3e8d6', '{}', TO_DATE('1980-08-10', 'YYYY-MM-DD'), 'White British', 'Mr', 'ADNKELNA', 'FATALIE', 'English', 'Jewish', 'Male');
