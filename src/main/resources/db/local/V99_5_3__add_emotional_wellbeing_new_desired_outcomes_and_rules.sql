-- Emotional Wellbeing outcomes
INSERT INTO desired_outcome (id, description, service_category_id, deprecated_at)
VALUES ('be2cb69b-389c-457e-87d8-d756826abd82',
        'Service User improves one or more of: emotional wellbeing, resilience, coping mechanisms or self-confidence.',
        '8221a81c-08b2-4262-9c1a-0ab3c82cec8c', NULL),
       ('32f14f09-7928-43da-8ac1-3b8f7e2fa307',
        'Service User is supported to engage with other services that will improve their physical or mental wellbeing.',
        '8221a81c-08b2-4262-9c1a-0ab3c82cec8c', NULL),
-- Exceptions: Greater Manchester only,
       ('73732960-b654-4856-a32c-a12d9511a17b',
        'Service User who is not NFA or at immediate risk of homelessness is supported to secure and maintain suitable accommodation.',
        '8221a81c-08b2-4262-9c1a-0ab3c82cec8c', NULL),

INSERT INTO desired_outcome_filter_rule(id, desired_outcome_id, rule_type, match_type)
VALUES ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', '73732960-b654-4856-a32c-a12d9511a17b', 'INCLUDE',
    'contract_reference');

INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_BOLTON_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_BURY_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_MANCHESTER_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_OLDHAM_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_ROCHDALE_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_SALFORD_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_STOCKPORT_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_TAMESIDE_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_TRAFFORD_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN671065_WIGAN_PWB'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_1'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_2'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_3'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_4'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_5'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_6'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_7'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_8'),
       ('281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', 'DN547952_9'),


-- -- Uncomment to roll back to original state
-- DELETE
-- FROM desired_outcome
-- where id in
--       (
--        'be2cb69b-389c-457e-87d8-d756826abd82',
--        '32f14f09-7928-43da-8ac1-3b8f7e2fa307',
--        '73732960-b654-4856-a32c-a12d9511a17b',
--           );
--
-- UPDATE desired_outcome
-- SET deprecated_at = null
-- where id IN
--       (
--        '52a658f6-622b-491e-8085-cea58c9b015b',
--        '2b6e54d3-1cf7-4fc7-aae2-2f338085feda',
--        '4fc163c9-f8fb-4cfd-b107-2cb13583ebf0',
--        'ce7a55cc-d179-49d8-b7ab-6e8d93024ead',
--        '117832ca-f65e-4cce-9cdd-bdf666e740ea',
--        '6277c89b-28ca-49bd-8411-bf3d461a96f2'
--           );