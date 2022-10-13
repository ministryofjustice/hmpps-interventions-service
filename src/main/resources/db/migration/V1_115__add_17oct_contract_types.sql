INSERT INTO contract_type (id, name, code)
VALUES ('a19f3da7-c5eb-4936-9f76-f877ddf5c656', 'Dependency and Recovery (West Midlands)', 'DNR-WM')
;

INSERT INTO service_category (id, created, name)
VALUES ('57d3cedb-29f8-4316-9c22-9d6d0631ffab', NOW(), 'Dependency and Recovery (West Midlands)')
;

INSERT INTO contract_type_service_category (contract_type_id, service_category_id)
VALUES ('a19f3da7-c5eb-4936-9f76-f877ddf5c656', '57d3cedb-29f8-4316-9c22-9d6d0631ffab') -- D&R West Midlands
; 

INSERT INTO complexity_level (id, service_category_id, title, description)
-- PRJ_7680 - D&R West Midlands - Low, Medium & High
VALUES ('795a70a5-46af-4608-990e-b54ea5136a21', '57d3cedb-29f8-4316-9c22-9d6d0631ffab', 'Low complexity',
        'Person(s) on Probation who is highly motivated and has had sustained period of abstinence. Person(s) on Probation has successfully engaged in a period of treatment but requires support to sustain recovery.')
     , ('b5670e26-3bc4-4164-81da-ba00e894ae03', '57d3cedb-29f8-4316-9c22-9d6d0631ffab', 'Medium complexity',
        'Person(s) on Probations who has had lapses/crises that may lead to relapse and/or is at a transition point, e.g. from prison into community. Person(s) on Probation has a moderate level of motivation but requires a high level of support for dependencies linked to re-offending. Person(s) on Probations who are subject to a Community Sentence Treatment Requirement and need wraparound support to aid engagement. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement.')
     , ('56c9d08c-239f-4f12-8f76-306d34028054', '57d3cedb-29f8-4316-9c22-9d6d0631ffab', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')
;

INSERT INTO desired_outcome (id, service_category_id, description)
-- PRJ_7680 - D&R West Midlands
VALUES ('b0678d77-70f4-4ebe-ac77-20f766c25299', '57d3cedb-29f8-4316-9c22-9d6d0631ffab',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('f3f63f51-332a-41df-a1e7-8209769e515a', '57d3cedb-29f8-4316-9c22-9d6d0631ffab',
        'Service user improves their physical health and mental resilience.')
     , ('1ebe9bb4-556b-4270-9046-cd56a7ffb583', '57d3cedb-29f8-4316-9c22-9d6d0631ffab',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('3a5e040c-4804-4d8a-90c7-2f44f9cd182d', '57d3cedb-29f8-4316-9c22-9d6d0631ffab',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('e6f9f8c6-65f7-4e2a-b435-dc8f5b2cdbc5', '57d3cedb-29f8-4316-9c22-9d6d0631ffab',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('63df6b54-e2bb-4d06-af76-24618a18d529', '57d3cedb-29f8-4316-9c22-9d6d0631ffab',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
;
