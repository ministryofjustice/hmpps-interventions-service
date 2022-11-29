INSERT INTO contract_type (id, name, code)
VALUES ('bb6b0997-a9bd-4d90-b908-15606f61d230', 'Finance, Benefit and Debt (North West)', 'FBD-NW')
;

INSERT INTO service_category (id, created, name)
VALUES ('cd362970-3fa5-4096-9f51-82dfd7dd8efb', NOW(), 'Finance, Benefit and Debt (North West)')
;

INSERT INTO contract_type_service_category (contract_type_id, service_category_id)
VALUES ('bb6b0997-a9bd-4d90-b908-15606f61d230', 'cd362970-3fa5-4096-9f51-82dfd7dd8efb')-- FBD North West
; 

INSERT INTO complexity_level (id, service_category_id, title, description)
-- PRJ_7648 - FBD North West
VALUES ('78266d2e-5a11-4ffe-9461-7c58fa7f1257', 'cd362970-3fa5-4096-9f51-82dfd7dd8efb', 'High complexity',
        'Person(s) referred Probation has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account.Person(s) on Probation circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs.Person(s) on Probation may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.Person(s) in prison requires a minimum of 3 face to face sessions')
;

INSERT INTO desired_outcome (id, service_category_id, description)
-- PRJ_7648 - FBD North West
VALUES ('a0e18ea2-9982-43c5-9155-8a07ec88fe75', 'cd362970-3fa5-4096-9f51-82dfd7dd8efb',
'the person referred’s financial management skills are developed and/or enhanced, including online banking skills')
, ('39393e5f-48d2-4981-84d1-e23d762f65b8', 'cd362970-3fa5-4096-9f51-82dfd7dd8efb',
'the person referred can successfully navigate the benefits system')
, ('af1d6c9c-bcdd-4e9f-a778-5c1a1e90163c', 'cd362970-3fa5-4096-9f51-82dfd7dd8efb',
'the person referred is supported to complete tasks that they would not otherwise be able to do whilst being in custody e.g. support with banking or debt management')
;
