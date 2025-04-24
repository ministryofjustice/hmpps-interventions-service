-- Service Pathway: Family and Significant Others,
INSERT INTO desired_outcome (id, description, service_category_id, deprecated_at)
VALUES
       ('686ce63b-6070-4471-b001-4256f95be078',
        'Family and significant others are supported to help service users comply with their licence or community order.',
        '9556a399-3529-4993-8030-41db2090555e', NULL),
       ('f265b88b-c79c-4860-a942-6db392e4b986', 'Service User demonstrates confident and responsible parenting behaviours.',
        '9556a399-3529-4993-8030-41db2090555e', NULL),
       ('ad3b053e-eb32-429f-958c-7f164dd2b7d2', 'Service User improves relationships with family and/or significant others.',
        '9556a399-3529-4993-8030-41db2090555e', NULL),
       ('edaca79a-9c43-44ad-996e-e4df789a9da0',
        'Service User demonstrates ability to respond to breakdown of family and other relationships.',
        '9556a399-3529-4993-8030-41db2090555e', NULL),
       ('04442985-3395-476f-93b7-6150bc78174e', 'Service User engages with voluntary or statutory family services.',
        '9556a399-3529-4993-8030-41db2090555e', NULL),

-- Family and Significant Others (GM)
       ('7e6c886d-e23b-4a48-8709-2114aad951b0',
        'Family and significant others are supported to help service users comply with their licence or community order.',
        '9232541b-6b1c-455d-8153-ab2784bf4593', NULL),
       ('1ba4a36e-c295-4316-aa74-cd6bc73b1f56', 'Service User demonstrates confident and responsible parenting behaviours.',
        '9232541b-6b1c-455d-8153-ab2784bf4593', NULL),
       ('50053ce6-34ab-40f8-a0f3-db2470185760', 'Service User improves relationships with family and/or significant others.',
        '9232541b-6b1c-455d-8153-ab2784bf4593', NULL),
       ('36136463-8334-42e6-b24d-8257dc6ed260',
        'Service User demonstrates ability to respond to breakdown of family and other relationships.',
        '9232541b-6b1c-455d-8153-ab2784bf4593', NULL),
       ('ab0da71b-34e9-459c-a20b-d561c807dae9', 'Service User engages with voluntary or statutory family services.',
        '9232541b-6b1c-455d-8153-ab2784bf4593', NULL),


INSERT INTO desired_outcome_filter_rule(id, desired_outcome_id, rule_type, match_type)
VALUES ('340e7a0a-ed98-430c-9c06-814936dc0456', '686ce63b-6070-4471-b001-4256f95be078', 'INCLUDE',
    'contract_reference'),
    ('3fd616f3-805c-4b32-afa3-f7514d3629d2', '7e6c886d-e23b-4a48-8709-2114aad951b0', 'INCLUDE',
    'contract_reference');

INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_BOLTON_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_BURY_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_MANCHESTER_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_OLDHAM_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_ROCHDALE_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_SALFORD_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_STOCKPORT_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_TAMESIDE_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_TRAFFORD_PWB'),
       ('340e7a0a-ed98-430c-9c06-814936dc0456', 'DN671065_WIGAN_PWB');


INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_1'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_2'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_3'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_4'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_5'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_6'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_7'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_8'),
       ('3fd616f3-805c-4b32-afa3-f7514d3629d2', 'DN547952_9');


-- -- Uncomment to roll back to original state
-- DELETE
-- FROM desired_outcome
-- where id in
--       (
--        '686ce63b-6070-4471-b001-4256f95be078',
--        'f265b88b-c79c-4860-a942-6db392e4b986',
--        'ad3b053e-eb32-429f-958c-7f164dd2b7d2',
--        'edaca79a-9c43-44ad-996e-e4df789a9da0',
--        '04442985-3395-476f-93b7-6150bc78174e',
--        '7e6c886d-e23b-4a48-8709-2114aad951b0',
--        '1ba4a36e-c295-4316-aa74-cd6bc73b1f56',
--        '50053ce6-34ab-40f8-a0f3-db2470185760',
--        '36136463-8334-42e6-b24d-8257dc6ed260',
--        'ab0da71b-34e9-459c-a20b-d561c807dae9'
--           );
--
-- UPDATE desired_outcome
-- SET deprecated_at = null
-- where id IN
--       (
--         '518c70c3-8f42-4ad6-a50d-f9e92d366059',
--         '9823356e-ff3b-4b29-9d6e-bc3065b067c1',
--         'bcb7309f-62a8-49ba-894d-3e627f0993e0',
--         '6f97a384-457d-4fc9-aa09-e30dad38d6bc',
--         '169824c2-ae25-4392-bf59-c74ae4591e30',
--         'd2df75af-0850-4e65-bd86-22b8f34067d3',
--         '1ffe903f-0aea-4ad0-8fe2-6e58b7ec0324',
--         '868f713a-5c53-4a18-b20f-ff8f118e9c30',
--         'dd7e2bb6-4374-405a-9524-432aefd63a70',
--         'fdf33edf-dd73-4649-88d5-330805124dd7',
--         'f2a1026d-dcb7-4d8b-a227-3f6615ca3b80'
--           );
