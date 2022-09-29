INSERT INTO contract_type (id, name, code)
VALUES ('8ecbf69b-9b54-49d8-a2bc-54c7b7cc4731', 'Finance, Benefit and Debt in Custody (North East)', 'FBD-NE')
     , ('588fea26-dae1-412e-add6-73e4e6d84a3c', 'Dependency and Recovery (North East: Northumbria)', 'DNR-NEN')
     , ('5379196d-559a-4709-bf37-fe6a6d65d9b5', 'Finance, Benefit and Debt (Wales)', 'FBD-W')
;

INSERT INTO service_category (id, created, name)
VALUES ('07e92c9c-51a6-43bb-81a9-226f518dd5ad', NOW(), 'Finance, Benefit and Debt in Custody (North East)')
     , ('748732c2-7f94-4966-b5f2-d19e86e0cbf2', NOW(), 'Dependency and Recovery (North East: Northumbria)')
     , ('cf2946a3-6bf2-4f0c-9154-190c2b2f899f', NOW(), 'Finance, Benefit and Debt (Wales)')
;

INSERT INTO contract_type_service_category (contract_type_id, service_category_id)
VALUES ('8ecbf69b-9b54-49d8-a2bc-54c7b7cc4731', '07e92c9c-51a6-43bb-81a9-226f518dd5ad') -- FBD North East
     , ('588fea26-dae1-412e-add6-73e4e6d84a3c', '748732c2-7f94-4966-b5f2-d19e86e0cbf2') -- DNR North East(Northumbria)
     , ('5379196d-559a-4709-bf37-fe6a6d65d9b5', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f') -- FBD Wales
; 

INSERT INTO complexity_level (id, service_category_id, title, description)
-- PRJ_7672 - D&R Northumbria Community only - High
VALUES ('dd39a06e-378a-4b40-99ca-7b9a11507104', '748732c2-7f94-4966-b5f2-d19e86e0cbf2', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')
-- PRJ_7659 - FBD Wales - Low & High
    , ('715b8c25-a9c9-40a8-8d9f-8b7bec38f943', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f', 'Low complexity',
        'Person(s) on Probation has prior experience of successfully dealing with their own finance, benefit and debt needs. Person(s) on Probations circumstances may be currently leading to significant financial pressure. Person(s) on Probation will require some advice, guidance and/or support across some specific but limited finance, benefit and debt needs, but will be able to support themselves with other aspects.')
    , ('69dabf1f-7dfe-40ee-946b-b107dca30912', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f', 'High complexity',
        'Person(s) on Probation has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account. Person(s) on Probations circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs. Person(s) on Probation may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.')
-- PRJ_7647 FBD NE - High
    , ('f7438e54-2500-4850-9c45-a25195ab70d4', '07e92c9c-51a6-43bb-81a9-226f518dd5ad', 'High complexity',
        ' Person(s) referred Probation has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account. Person(s) on Probation circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs. Person(s) on Probation may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.')
;

INSERT INTO desired_outcome (id, service_category_id, description)
-- PRJ_7672 - D&R Northumbria Community only
VALUES ('79e00aae-c8e5-4d9d-8e49-55d3f64cf84d', '748732c2-7f94-4966-b5f2-d19e86e0cbf2',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('458ec1ed-930c-4af9-9acf-b7303ed84c65', '748732c2-7f94-4966-b5f2-d19e86e0cbf2',
        'Service user improves their physical health and mental resilience.')
     , ('a1e43eeb-72ad-413b-af3e-03383d7bfe1a', '748732c2-7f94-4966-b5f2-d19e86e0cbf2',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('9ac26331-b390-486e-be2b-3e9a07c366e1', '748732c2-7f94-4966-b5f2-d19e86e0cbf2',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('ab96749c-8c15-4b5c-94d7-666432218bf9', '748732c2-7f94-4966-b5f2-d19e86e0cbf2',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('d51618fa-ec50-47de-8438-8c7ba75c5992', '748732c2-7f94-4966-b5f2-d19e86e0cbf2',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
-- PRJ_7659 - FBD Wales
    , ('ec9b60e2-7732-4747-94c7-f3904d5fb3d4', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f',
        'Service user has access to appropriate financial products, advice and/or services.')
     , ('ba5c6553-f2c5-4b2b-8a2f-7f6aec266aa2', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f',
        'Pathways are established to help service user maintain and sustain an income, safely manage money and reduce debt.')
     , ('8e40c026-b7b5-4d20-bbc9-891f697a70fa', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f',
        'Service user on probation financial management skills are developed and/or enhanced including online banking.')
     , ('95370c1b-e4db-403d-b595-d22105f9c479', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f',
        'Service user can successfully navigate the benefits system.')
     , ('a670adae-9e36-434d-8c4d-1ebdda3f7b15', 'cf2946a3-6bf2-4f0c-9154-190c2b2f899f',
        'Service user gains quick access to universal credit.')
-- PRJ_7647 FBD NE
     , ('6ba59e39-6c9d-4aaf-bf45-76b55f151a3f', '07e92c9c-51a6-43bb-81a9-226f518dd5ad',
        'The person referred’s financial management skills are developed and/or enhanced, including online banking skills.')
     , ('715c533a-ce87-4f0a-8d63-67fdca4df3db', '07e92c9c-51a6-43bb-81a9-226f518dd5ad',
        'The person referred can successfully navigate the benefits system.')
     , ('2054a511-7f9c-484b-a80d-512e489c1cc5', '07e92c9c-51a6-43bb-81a9-226f518dd5ad',
        'Pathways are established to help the person referred to maintain and sustain an income, safely manage money and reduce debt.')
     , ('a1352f1d-16a5-47b4-a9d3-fbdb25831ea0', '07e92c9c-51a6-43bb-81a9-226f518dd5ad',
        'The person referred has access to appropriate financial products, advice and/or services')
     , ('df778b16-8d84-47af-8e34-0c4e680e9f0a', '07e92c9c-51a6-43bb-81a9-226f518dd5ad',
        'The person referred gains quick access to universal credit, including pre-release referrals.')
     , ('bf74188a-117a-473b-bb79-970f88b2805c', '07e92c9c-51a6-43bb-81a9-226f518dd5ad',
        'The person referred is supported to complete tasks that they would not otherwise be able to do whilst being in custody e.g. support with banking or debt management.')
;
