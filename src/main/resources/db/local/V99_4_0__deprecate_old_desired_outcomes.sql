INSERT INTO desired_outcome(id, description, service_category_id, deprecated_at)
VALUES
    -- Accommodation new desired outcomes
    ('3ecf4b60-7973-419a-b8c3-ef54b078ef00',
     'Service User is helped to secure social, supported housing or a tenancy in the PRS.',
     '428ee70f-3001-4399-95a6-ad25eaaede16', '2025-10-10 00:00:00.000000 +00:00'),
    ('0092ec26-4c2a-4eb9-9b70-687e9936d52b',
     'Service User is prevented from losing existing accommodation or becoming homeless.',
     '428ee70f-3001-4399-95a6-ad25eaaede16', '2025-10-10 00:00:00.000000 +00:00'),
    ('efe6463f-20da-4ab3-b1b4-74b785be8e5d',
     'Service User makes progress towards obtaining accommodation, through removal of barriers or active steps to secure accommodation.',
     '428ee70f-3001-4399-95a6-ad25eaaede16', '2025-10-10 00:00:00.000000 +00:00');

-- Un-deprecates new outcomes
UPDATE desired_outcome
SET deprecated_at = null
where id IN ('3ecf4b60-7973-419a-b8c3-ef54b078ef00',
             '0092ec26-4c2a-4eb9-9b70-687e9936d52b',
             'efe6463f-20da-4ab3-b1b4-74b785be8e5d');

-- Deprecates old outcomes
UPDATE desired_outcome
SET deprecated_at = '2025-04-03 00:00:00.000000 +00:00'
where id IN (
             '301ead30-30a4-4c7c-8296-2768abfb59b5',
             '65924ac6-9724-455b-ad30-906936291421',
             '9b30ffad-dfcb-44ce-bdca-0ea49239a21a',
             'e7f199de-eee1-4f57-a8c9-69281ea6cd4d',
             '19d5ef58-5cfc-41fe-894c-acd705dc1325',
             'f6f70273-16a2-4dc7-aafc-9bc74215e713',
             '55a9cf76-428d-4409-8a57-aaa523f3b631',
             '449a93d7-e705-4340-9936-c859644abd52'
    );

-- -- Uncomment to roll back desired outcomes to original state
-- -- New outcomes
-- UPDATE desired_outcome
-- SET deprecated_at = '2025-10-10 00:00:00.000000 +00:00'
-- where id IN ('3ecf4b60-7973-419a-b8c3-ef54b078ef00',
--              '0092ec26-4c2a-4eb9-9b70-687e9936d52b',
--              'efe6463f-20da-4ab3-b1b4-74b785be8e5d');
-- -- Old outcomes
-- UPDATE desired_outcome
-- SET deprecated_at = null
-- where id IN (
--              '301ead30-30a4-4c7c-8296-2768abfb59b5',
--              '65924ac6-9724-455b-ad30-906936291421',
--              '9b30ffad-dfcb-44ce-bdca-0ea49239a21a',
--              'e7f199de-eee1-4f57-a8c9-69281ea6cd4d',
--              '19d5ef58-5cfc-41fe-894c-acd705dc1325',
--              'f6f70273-16a2-4dc7-aafc-9bc74215e713',
--              '55a9cf76-428d-4409-8a57-aaa523f3b631',
--              '449a93d7-e705-4340-9936-c859644abd52'
--     );

-- Uncomment to remove new desired outcomes
-- DELETE
-- FROM desired_outcome
-- where id IN ('3ecf4b60-7973-419a-b8c3-ef54b078ef00',
--              '0092ec26-4c2a-4eb9-9b70-687e9936d52b',
--              'efe6463f-20da-4ab3-b1b4-74b785be8e5d');