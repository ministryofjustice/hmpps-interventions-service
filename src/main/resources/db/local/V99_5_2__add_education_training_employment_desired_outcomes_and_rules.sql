-- Service Pathway: Eduction, Training and Employment

INSERT INTO desired_outcome (id, description, service_category_id, deprecated_at)
VALUES
    ('53eff054-63a4-4776-8c99-b17c942c4f6f', 'Service User increases their employability.',
     'ca374ac3-84eb-4b91-bea7-9005398f426f', '2025-10-10 00:00:00.000000 +00:00'),
    ('e0d5bf23-b2ad-4753-866a-d76e072b2c09', 'Service User commences suitable Education, Training & Employment.',
     'ca374ac3-84eb-4b91-bea7-9005398f426f', '2025-10-10 00:00:00.000000 +00:00'),
    ('804b586c-59cb-4fe0-80a0-34500ea24fc9', 'Service User maintains suitable Education, Training & Employment.',
     'ca374ac3-84eb-4b91-bea7-9005398f426f', '2025-10-10 00:00:00.000000 +00:00');

UPDATE desired_outcome
SET deprecated_at = '2025-04-03 00:00:00.000000 +00:00'
where id IN (
             '8f438eb1-4f5c-4c23-9436-2ef784fb2426',
             'd45e5b8f-7ae3-4fed-8c6b-73959cfc1190',
             '42077326-ca07-40ea-af20-902249af844e',
             '0541193e-ba84-46e7-973f-82a9900d7521'
    );


-- -- Uncomment to roll back to original state
-- DELETE
-- FROM desired_outcome
-- where id in
--       (
--        '53eff054-63a4-4776-8c99-b17c942c4f6f',
--        'e0d5bf23-b2ad-4753-866a-d76e072b2c09',
--        '804b586c-59cb-4fe0-80a0-34500ea24fc9'
--           );
--
-- UPDATE desired_outcome
-- SET deprecated_at = null
-- where id IN
--       (
--         '8f438eb1-4f5c-4c23-9436-2ef784fb2426'
--         'd45e5b8f-7ae3-4fed-8c6b-73959cfc1190',
--         '42077326-ca07-40ea-af20-902249af844e',
--         '0541193e-ba84-46e7-973f-82a9900d7521'
--           );

