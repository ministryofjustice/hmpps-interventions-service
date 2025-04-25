-- Dependency and Recovery outcomes
INSERT INTO desired_outcome (id, description, service_category_id, deprecated_at)
VALUES ('b59dbf80-f2b5-425b-8115-66eb8ad7e40d',
        'Service User has a minimised risk of overdose following release from prison.',
        '76bcdb97-1dea-41c1-a4f8-899d88e5d679', NULL),
       ('889f3021-af80-470b-8ea9-d4390ae0bbc6',
        'Service User has a minimised risk of overdose following release from prison.',
        '4adf3bd1-be2a-44dc-922d-8a915367b77c', NULL),
       ('3c4cd2c6-49cc-4962-8441-724d2baffb94',
        'Service User has a minimised risk of overdose following release from prison.',
        'b34846f0-176e-483b-92d9-6621dda855eb', NULL),
       ('eb241f98-3b7a-483d-9945-69cbb7588295',
        'Service User has a minimised risk of overdose following release from prison.',
        '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240', NULL),
       ('3d8fea6f-003a-49a5-a9d6-5cacc724966c',
        'Service User has a minimised risk of overdose following release from prison.',
        '748732c2-7f94-4966-b5f2-d19e86e0cbf2', NULL),
       ('86324d1b-31fb-4482-84d1-212372c9828d',
        'Service User has a minimised risk of overdose following release from prison.',
        'c3e9ef94-40fb-4284-8920-08ea6f8186e1', NULL),
       ('21277c11-68c5-4f7c-b6c9-6aa7fc320ca2',
        'Service User has a minimised risk of overdose following release from prison.',
        (select id from service_category where name = 'Dependency and Recovery (South West)'), NULL),
       ('c9c8a1ee-c2b3-4639-850b-48e0e139311c',
        'Service User has a minimised risk of overdose following release from prison.',
        '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3', NULL),
       ('7dc10fa2-3ba7-47ac-a93c-96f48754657b',
        'Service User has a minimised risk of overdose following release from prison.',
        '57d3cedb-29f8-4316-9c22-9d6d0631ffab', NULL),
       ('13abd131-a1b3-4232-b444-68263faf228e',
        'Service User has a minimised risk of overdose following release from prison.',
        'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a', NULL),
       ('118141da-d673-47de-84f9-ec28261ad112',
        'Service User accesses appropriate support, on release from custody or in the community.',
        '76bcdb97-1dea-41c1-a4f8-899d88e5d679', NULL),
       ('215dea2f-7839-4a4d-8fd5-0f041e25ab26',
        'Service User accesses appropriate support, on release from custody or in the community.',
        '4adf3bd1-be2a-44dc-922d-8a915367b77c', NULL),
       ('db872833-1f17-4122-89bf-148cb1881b2b',
        'Service User accesses appropriate support, on release from custody or in the community.',
        'b34846f0-176e-483b-92d9-6621dda855eb', NULL),
       ('56d3e648-9142-4e33-ba02-54c7a1b004cc',
        'Service User accesses appropriate support, on release from custody or in the community.',
        '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240', NULL),
       ('74e00e83-411a-4e2e-ad28-85fc28f5b83e',
        'Service User accesses appropriate support, on release from custody or in the community.',
        '748732c2-7f94-4966-b5f2-d19e86e0cbf2', NULL),
       ('c7bed839-192c-4cc3-abfd-5dcd9b89e0f1',
        'Service User accesses appropriate support, on release from custody or in the community.',
        'c3e9ef94-40fb-4284-8920-08ea6f8186e1', NULL),
       ('e44ae670-bc65-49e5-a72d-5155ab359ff1',
        'Service User accesses appropriate support, on release from custody or in the community.',
        (select id from service_category where name = 'Dependency and Recovery (South West)'), NULL),
       ('618dbbe3-b6fc-44ef-8245-02d42d2f867d',
        'Service User accesses appropriate support, on release from custody or in the community.',
        '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3', NULL),
       ('60bffe9b-5307-4579-94bf-ef58df889c8e',
        'Service User accesses appropriate support, on release from custody or in the community.',
        '57d3cedb-29f8-4316-9c22-9d6d0631ffab', NULL),
       ('d1985e92-6a9f-4919-bed4-d4aa55000496',
        'Service User accesses appropriate support, on release from custody or in the community.',
        'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a', NULL),
       ('28c62871-540d-4073-aff0-5fd8242a2baa',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        '76bcdb97-1dea-41c1-a4f8-899d88e5d679', NULL),
       ('d96fdc8b-5e65-4f85-9c58-99bbf38d7233',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        '4adf3bd1-be2a-44dc-922d-8a915367b77c', NULL),
       ('4b6e269e-68a7-48aa-bfaa-8c3421fb7151',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        'b34846f0-176e-483b-92d9-6621dda855eb', NULL),
       ('6558b4b5-6acf-4409-84bf-eea908660658',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240', NULL),
       ('c2ed44f0-535d-4488-aef9-8cbc71b18aa9',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        '748732c2-7f94-4966-b5f2-d19e86e0cbf2', NULL),
       ('84f02517-5cd5-4260-9c43-896cfefa5af2',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        'c3e9ef94-40fb-4284-8920-08ea6f8186e1', NULL),
       ('c0a02cd3-db90-4485-b517-1c5bde8b91a8',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        (select id from service_category where name = 'Dependency and Recovery (South West)'), NULL),
       ('16684f75-d42c-4d37-ba74-b9fba071eb0f',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        '1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3', NULL),
       ('d4f339bd-30b0-462e-94bf-b955b8c780ee',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        '57d3cedb-29f8-4316-9c22-9d6d0631ffab', NULL),
       ('cdee3126-44b8-40d6-a718-8b315115a1ae',
        'Service User achieves non-dependent or non-problematic substance misuse.',
        'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a', NULL);

INSERT INTO desired_outcome_filter_rule(id, desired_outcome_id, rule_type, match_type)
VALUES ('e866ff81-49ee-443d-87b6-e14079def942', 'b59dbf80-f2b5-425b-8115-66eb8ad7e40d', 'INCLUDE','contract_reference'),
       ('cd291652-d699-4373-831b-e98487be5b6e', '889f3021-af80-470b-8ea9-d4390ae0bbc6', 'INCLUDE','contract_reference'),
       ('d0b94370-464c-49a0-bb99-aaed15ed292e', '3c4cd2c6-49cc-4962-8441-724d2baffb94', 'INCLUDE','contract_reference'),
       ('bdfc9390-e493-4f22-b3d6-4b7f918b1f29', 'eb241f98-3b7a-483d-9945-69cbb7588295', 'INCLUDE','contract_reference'),
       ('8c72d731-4e44-4ebe-a970-49fc118e3718', '3d8fea6f-003a-49a5-a9d6-5cacc724966c', 'INCLUDE','contract_reference'),
       ('6427c3c4-4d7b-482b-8d7a-607e09dba4a5', '86324d1b-31fb-4482-84d1-212372c9828d', 'INCLUDE','contract_reference'),
       ('7b7b21ca-2ea0-48e7-b7e1-ba1a9ea2cb64', '21277c11-68c5-4f7c-b6c9-6aa7fc320ca2', 'INCLUDE','contract_reference'),
       ('54cbbb0e-9fb4-46d7-bddf-56b4e505848d', 'c9c8a1ee-c2b3-4639-850b-48e0e139311c', 'INCLUDE','contract_reference'),
       ('c29a753e-719b-4540-9920-b78b7558ae17', '7dc10fa2-3ba7-47ac-a93c-96f48754657b', 'INCLUDE','contract_reference'),
       ('54c26c61-0ca6-46c8-85ea-51d04d2b164c', '13abd131-a1b3-4232-b444-68263faf228e', 'INCLUDE','contract_reference');

-- SC, 76bcdb97-1dea-41c1-a4f8-899d88e5d679, b59dbf80-f2b5-425b-8115-66eb8ad7e40d
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('e866ff81-49ee-443d-87b6-e14079def942', 'DN547952_1');

-- SC, 4adf3bd1-be2a-44dc-922d-8a915367b77c
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('cd291652-d699-4373-831b-e98487be5b6e', '');

-- b34846f0-176e-483b-92d9-6621dda855eb
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('d0b94370-464c-49a0-bb99-aaed15ed292e', '');

-- 3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('bdfc9390-e493-4f22-b3d6-4b7f918b1f29', '');

-- 748732c2-7f94-4966-b5f2-d19e86e0cbf2
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('8c72d731-4e44-4ebe-a970-49fc118e3718', '');

-- c3e9ef94-40fb-4284-8920-08ea6f8186e1
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('6427c3c4-4d7b-482b-8d7a-607e09dba4a5', '');

-- (select id from service_category where name = 'Dependency and Recovery (South West)')
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('7b7b21ca-2ea0-48e7-b7e1-ba1a9ea2cb64', '');

-- 1f33f0b8-0d64-4dce-aa20-4cd70e11ddd3
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('54cbbb0e-9fb4-46d7-bddf-56b4e505848d', '');

-- 57d3cedb-29f8-4316-9c22-9d6d0631ffab
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('c29a753e-719b-4540-9920-b78b7558ae17', '');

-- ca13b995-4c91-4d0a-9fe8-2055cc0acb1a
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
VALUES ('54c26c61-0ca6-46c8-85ea-51d04d2b164c', '');



-- -- Uncomment to roll back to original state
-- DELETE
-- FROM desired_outcome
-- where id in
--       (
--        'b59dbf80-f2b5-425b-8115-66eb8ad7e40d',
--        '889f3021-af80-470b-8ea9-d4390ae0bbc6',
--        '3c4cd2c6-49cc-4962-8441-724d2baffb94',
--        'eb241f98-3b7a-483d-9945-69cbb7588295',
--        '3d8fea6f-003a-49a5-a9d6-5cacc724966c',
--        '86324d1b-31fb-4482-84d1-212372c9828d',
--        '21277c11-68c5-4f7c-b6c9-6aa7fc320ca2',
--        'c9c8a1ee-c2b3-4639-850b-48e0e139311c',
--        '7dc10fa2-3ba7-47ac-a93c-96f48754657b',
--        '13abd131-a1b3-4232-b444-68263faf228e',
--        '118141da-d673-47de-84f9-ec28261ad112',
--        '215dea2f-7839-4a4d-8fd5-0f041e25ab26',
--        'db872833-1f17-4122-89bf-148cb1881b2b',
--        '56d3e648-9142-4e33-ba02-54c7a1b004cc',
--        '74e00e83-411a-4e2e-ad28-85fc28f5b83e',
--        'c7bed839-192c-4cc3-abfd-5dcd9b89e0f1',
--        'e44ae670-bc65-49e5-a72d-5155ab359ff1',
--        '618dbbe3-b6fc-44ef-8245-02d42d2f867d',
--        '60bffe9b-5307-4579-94bf-ef58df889c8e',
--        'd1985e92-6a9f-4919-bed4-d4aa55000496',
--        '28c62871-540d-4073-aff0-5fd8242a2baa',
--        'd96fdc8b-5e65-4f85-9c58-99bbf38d7233',
--        '4b6e269e-68a7-48aa-bfaa-8c3421fb7151',
--        '6558b4b5-6acf-4409-84bf-eea908660658',
--        'c2ed44f0-535d-4488-aef9-8cbc71b18aa9',
--        '84f02517-5cd5-4260-9c43-896cfefa5af2',
--        'c0a02cd3-db90-4485-b517-1c5bde8b91a8',
--        '16684f75-d42c-4d37-ba74-b9fba071eb0f',
--        'd4f339bd-30b0-462e-94bf-b955b8c780ee',
--        'cdee3126-44b8-40d6-a718-8b315115a1ae'
--           );
--
-- UPDATE desired_outcome
-- SET deprecated_at = null
-- where id IN
--       (
--        '6695be52-7230-4ee8-924b-4ce811f687c2', '1b90c7f2-6cb2-44b0-b317-ec7c8f9fc44b',
--        'c5213107-540f-4685-a08f-b3a0fb300030', '29b64d0b-5778-4a4f-a743-2dc78cadc1c0',
--        '587c5ab7-eeb1-465e-a4be-4815b93c6bfd', '39bfa839-efb4-474a-97dc-3c7fe23099a9',
--        'c7e3309e-35a5-4f9f-8658-a9fb84667435', 'a50a7ab9-6812-4b25-9a5e-790b2cd37fcf',
--        'e0a85ac2-ae2a-46c9-b45c-72aa960ebd7e', '589c6fe5-455f-48a9-8305-d992fc96f8da',
--        '9dd34514-c0c2-4f00-9ec2-e59afe6ac2d9', '2fd7de2c-8746-4a4d-9d09-2242c8959858',
--        'f7c8cd72-3d83-4dfc-9349-13895e9545ae', 'd5777eb0-bdd4-4aab-88c4-db62d9c06dcc',
--        '5f671fcb-cfba-49e3-92db-09b9b443a973', 'e935da84-ecbc-4c51-976a-04de1f9ddacc',
--        '2df12ce0-17cc-40f4-a6d9-7b71376aced1', '5696c601-a45c-48b3-99d2-3b6ea7cb4559',
--        'cd95d8a6-8d7d-4b9d-9546-6d22b47780ef', '4b23cac5-fb44-42e0-a9ea-dd220697e1f6',
--        '3fa5d3ee-3072-417d-89f3-06e1751d3c3e', 'ef3b98e6-ea1b-45f3-bff3-7bef23a9bfdf',
--        '03da060f-7559-4c73-a6d9-284e7503feb0', '4fe2e6e2-8e24-4a17-ac3a-9ccbc2aca394',
--        '5b68b729-4820-4551-b59a-aea084518b69', '95fefcab-8bc7-43de-ade0-31b3e823b269',
--        'a8f3ed4f-d29a-45ff-8b83-3b0e16c620b9', '7e98f2c1-8f9a-4077-9c68-bb0a8df2dae4',
--        '4e6c706f-8ffc-401c-819f-b0e06e0d4882', '6272576b-b8ad-4def-a965-920703aa1fcb',
--        'a3a7bdd5-9195-4a47-aadb-767aa37c9eba', 'a5781985-921f-4ada-bc08-b446952ee6a2',
--        '9f555e20-849d-4510-b08a-9a5b25ddfcfa', 'f19e9d10-a8ac-4292-ba25-22b71e93abd5',
--        '91924443-c341-4585-8acd-7f9c3def02ac', '399a5888-52b1-4c61-a4c8-52f1b80005a5',
--        '33a352d3-033b-4d3e-a5bb-d18405ca9bb3', '07daeabc-fb93-48e9-bb12-bdfc9f922c46',
--        '6439563a-9553-4e4d-aed8-5012765bae03', '423caf07-f394-4ec3-b21f-8d80db946b34',
--        'b0678d77-70f4-4ebe-ac77-20f766c25299', 'f3f63f51-332a-41df-a1e7-8209769e515a',
--        '1ebe9bb4-556b-4270-9046-cd56a7ffb583', '3a5e040c-4804-4d8a-90c7-2f44f9cd182d',
--        'e6f9f8c6-65f7-4e2a-b435-dc8f5b2cdbc5', '63df6b54-e2bb-4d06-af76-24618a18d529',
--        '8341f53f-2e16-4af2-b928-92c9b24c902a', 'ccc39f3c-a2d0-484b-81f3-0db847e337a8',
--        '7f38a11e-8375-491d-9b61-fc28c397cb6b', '535793e3-073e-47ff-a701-63942d0026b8',
--        'c8f7b418-2d54-4f3c-8beb-c9ce2b4ca34e', 'b8219e7a-8ca0-4ea4-a577-3cbb0aae34f9',
--        '9462c7cb-0305-40e8-a68c-cf92b9dbda94', '99de9424-c54a-40fd-b32c-da51fb9b126b',
--        '5783aa24-48ac-430a-8143-87a0de663152', '79e00aae-c8e5-4d9d-8e49-55d3f64cf84d',
--        '458ec1ed-930c-4af9-9acf-b7303ed84c65', 'a1e43eeb-72ad-413b-af3e-03383d7bfe1a',
--        '9ac26331-b390-486e-be2b-3e9a07c366e1', 'ab96749c-8c15-4b5c-94d7-666432218bf9',
--        'd51618fa-ec50-47de-8438-8c7ba75c5992', 'f003c982-6f8a-4d7c-b1f4-d39d025580a2',
--        '024e3e00-eb3b-463f-aa4a-adf51a2762f0', 'e3d99ba0-a43a-4eba-82bb-9bbf8e0213fc',
--        'b5cd3823-1f46-4fbf-ad8d-caf3212a9d49', '3cdeb455-9ce6-4f16-9942-9792abdac2a1',
--        'b274535a-5617-4ccf-9f70-7d52320f7db2'
--           );