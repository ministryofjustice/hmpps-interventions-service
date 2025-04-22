-- Inserts for Service Pathway: Education, Training and Employment
INSERT INTO desired_outcome (id,
                             description,
                             service_category_id,
                             deprecated_at)
VALUES ('b0b27ea1-1ad3-4273-b45d-239f8e6466d3',
        'Service User increases their employability.',
        'ca374ac3-84eb-4b91-bea7-9005398f426f',
        NULL),
       ('9a7acb60-a634-45d8-bd27-b65b6ec4f185',
        'Service User commences suitable Education, Training & Employment.',
        'ca374ac3-84eb-4b91-bea7-9005398f426f',
        NULL),
       ('f2913c23-8d8a-4dc4-8b9e-dde5cff87870',
        'Service User maintains suitable Education, Training & Employment.',
        'ca374ac3-84eb-4b91-bea7-9005398f426f',
        NULL);

-- Deprecates old outcomes
UPDATE desired_outcome
SET deprecated_at = '2025-04-03 00:00:00.000000 +00:00'
where id IN ('8f438eb1-4f5c-4c23-9436-2ef784fb2426', 'd45e5b8f-7ae3-4fed-8c6b-73959cfc1190',
             '42077326-ca07-40ea-af20-902249af844e', '0541193e-ba84-46e7-973f-82a9900d7521'
    );

-- -- Uncomment to roll back to original state
-- DELETE
-- FROM desired_outcome
-- where id in
--       (
--        'b0b27ea1-1ad3-4273-b45d-239f8e6466d3',
--        '9a7acb60-a634-45d8-bd27-b65b6ec4f185',
--        'f2913c23-8d8a-4dc4-8b9e-dde5cff87870'
--           );
--
-- UPDATE desired_outcome
-- SET deprecated_at = null
-- where id IN
--       (
--        '8f438eb1-4f5c-4c23-9436-2ef784fb2426', 'd45e5b8f-7ae3-4fed-8c6b-73959cfc1190',
--        '42077326-ca07-40ea-af20-902249af844e', '0541193e-ba84-46e7-973f-82a9900d7521'
--           );