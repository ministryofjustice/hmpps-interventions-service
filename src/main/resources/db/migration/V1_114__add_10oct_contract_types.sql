INSERT INTO contract_type (id, name, code)
VALUES ('d10c792d-12bd-4ac4-a999-04dbdbf59940', 'Dependency and Recovery (South Central)', 'DNR-SC')
     , ('bcb3eb1d-b95d-4679-bb85-b91aa3240ef5', 'Dependency and Recovery (South Yorkshire)', 'DNR-SY')
;

INSERT INTO service_category (id, created, name)
VALUES ('c3e9ef94-40fb-4284-8920-08ea6f8186e1', NOW(), 'Dependency and Recovery (South Central)')
     , ('1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3', NOW(), 'Dependency and Recovery (South Yorkshire)')
;

INSERT INTO contract_type_service_category (contract_type_id, service_category_id)
VALUES ('d10c792d-12bd-4ac4-a999-04dbdbf59940', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1') -- DNR South Central
     , ('bcb3eb1d-b95d-4679-bb85-b91aa3240ef5', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3') -- DNR South Yorkshire
; 

INSERT INTO complexity_level (id, service_category_id, title, description)
-- PRJ_7674 - D&R South Central (Hampshire) - Low, Medium & High
VALUES ('8d04a4c6-84d5-467f-8bb7-2fd74c859196', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1', 'Low complexity',
        'Person(s) on Probation who is highly motivated and has had sustained period of abstinence. Person(s) on Probation has successfully engaged in a period of treatment but requires support to sustain recovery.')
     , ('072379fa-73fe-4638-8bc5-0b1d76eb9d02', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1', 'Medium complexity',
        'Person(s) on Probations who has had lapses/crises that may lead to relapse and/or is at a transition point, e.g. from prison into community. Person(s) on Probation has a moderate level of motivation but requires a high level of support for dependencies linked to re-offending. Person(s) on Probations who are subject to a Community Sentence Treatment Requirement and need wraparound support to aid engagement. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement.')
     , ('3dd6d11c-bc46-4f8c-bd5b-8c6d73de04ce', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')
-- PRJ_7682 - D&R South Yorkshire - Medium & High
     , ('92e208c6-bbfd-4675-980f-f00a740e975a', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3', 'Medium complexity',
        'Person(s) on Probations who has had lapses/crises that may lead to relapse and/or is at a transition point, e.g. from prison into community. Person(s) on Probation has a moderate level of motivation but requires a high level of support for dependencies linked to re-offending. Person(s) on Probations who are subject to a Community Sentence Treatment Requirement and need wraparound support to aid engagement. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement.')
     , ('69e99592-07df-49ea-a562-705b7d5daaaa', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')
;

INSERT INTO desired_outcome (id, service_category_id, description)
-- PRJ_7674 - D&R South Central (Hampshire)
VALUES ('4e6c706f-8ffc-401c-819f-b0e06e0d4882', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('6272576b-b8ad-4def-a965-920703aa1fcb', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1',
        'Service user improves their physical health and mental resilience.')
     , ('a3a7bdd5-9195-4a47-aadb-767aa37c9eba', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('a5781985-921f-4ada-bc08-b446952ee6a2', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('9f555e20-849d-4510-b08a-9a5b25ddfcfa', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('f19e9d10-a8ac-4292-ba25-22b71e93abd5', 'c3e9ef94-40fb-4284-8920-08ea6f8186e1',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
-- PRJ_7682 - D&R South Yorkshire
     , ('91924443-c341-4585-8acd-7f9c3def02ac', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('399a5888-52b1-4c61-a4c8-52f1b80005a5', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3',
        'Service user improves their physical health and mental resilience.')
     , ('33a352d3-033b-4d3e-a5bb-d18405ca9bb3', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('07daeabc-fb93-48e9-bb12-bdfc9f922c46', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('6439563a-9553-4e4d-aed8-5012765bae03', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('423caf07-f394-4ec3-b21f-8d80db946b34', '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
;
