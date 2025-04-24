-- Service Pathway: Lifestyle and Associates

INSERT INTO desired_outcome (id, description, service_category_id, deprecated_at)
VALUES
    ('d0d077f9-9832-41ac-ad89-fc4a915dee0f',
     'Service User reduces engagement with pro-criminal and/or negative relationships or networks.',
     'b84f4eb7-4db0-477e-8c59-21027b3262c5', NULL),
    ('f7908f5e-ec66-43a1-8a1b-a36081efed04',
     'Service User engages in pro-social community-based support networks, relationships or activities.',
     'b84f4eb7-4db0-477e-8c59-21027b3262c5', NULL);

UPDATE desired_outcome
SET deprecated_at = '2025-04-03 00:00:00.000000 +00:00'
where id IN (
             'f45e26a8-65d4-4acf-bee4-fd6d0f5cf1e5',
             'f2aa2f28-27b5-4e79-856e-6a3f19550b20',
             'fdabad61-446a-47cc-b100-7a56b41f7812',
             'c19081d3-75f5-4ff1-bd37-e29a3bb1618d',
             'c8aa564d-12ba-4479-9f07-4a6a60fb3bbf'
    );

-- -- Uncomment to roll back to original state
-- DELETE
-- FROM desired_outcome
-- where id in
--       (
--        'd0d077f9-9832-41ac-ad89-fc4a915dee0f,
--        'f7908f5e-ec66-43a1-8a1b-a36081efed04'
--           );
--
-- UPDATE desired_outcome
-- SET deprecated_at = null
-- where id IN
--       (
--         'f45e26a8-65d4-4acf-bee4-fd6d0f5cf1e5'
--         'f2aa2f28-27b5-4e79-856e-6a3f19550b20',
--         'fdabad61-446a-47cc-b100-7a56b41f7812',
--         'c19081d3-75f5-4ff1-bd37-e29a3bb1618d',
--         'c8aa564d-12ba-4479-9f07-4a6a60fb3bbf'
--           );

