INSERT INTO contract_type (id, name, code)
VALUES ('c1053875-9492-48b3-94bc-ecde8c249b26', 'Dependency and Recovery (Yorkshire and the Humber)', 'DNR-Y')
     , ('c8a17e8a-1804-4c68-8109-f58447f5f06d', 'Dependency and Recovery (London)', 'DNR-L')
     , ('8028db0e-ee90-4982-b894-78f940ebd8c8', 'Dependency and Recovery (North East)', 'DNR-NE')
     , ('7af3a832-2b46-43b2-9db1-6bc43181a233', 'Dependency and Recovery (Kent, Surrey and Sussex)', 'DNR-K')
;

INSERT INTO service_category (id, created, name)
VALUES ('ca13b995-4c91-4d0a-9fe8-2055cc0acb1a', NOW(), 'Dependency and Recovery (Yorkshire and the Humber)')
     , ('b34846f0-176e-483b-92d9-6621dda855eb', NOW(), 'Dependency and Recovery (London)')
     , ('3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240', NOW(), 'Dependency and Recovery (North East)')
     , ('4adf3bd1-be2a-44dc-922d-8a915367b77c', NOW(), 'Dependency and Recovery (Kent, Surrey and Sussex)')
;

INSERT INTO contract_type_service_category (contract_type_id, service_category_id)
VALUES ('c1053875-9492-48b3-94bc-ecde8c249b26', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a') -- Yorkshire and the Humber
     , ('c8a17e8a-1804-4c68-8109-f58447f5f06d', 'b34846f0-176e-483b-92d9-6621dda855eb') -- London
     , ('8028db0e-ee90-4982-b894-78f940ebd8c8', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240') -- North East
     , ('7af3a832-2b46-43b2-9db1-6bc43181a233', '4adf3bd1-be2a-44dc-922d-8a915367b77c') -- Kent, Surrey and Sussex
; 

INSERT INTO complexity_level (id, service_category_id, title, description)
-- DNR Yorkshire and the Humber - Medium & High
VALUES ('45e2e712-4a66-48f0-a30c-dceff659cae9', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a', 'Medium complexity',
        'Person(s) on Probations who has had lapses/crises that may lead to relapse and/or is at a transition point, e.g. from prison into community. Person(s) on Probation has a moderate level of motivation but requires a high level of support for dependencies linked to re-offending. Person(s) on Probations who are subject to a Community Sentence Treatment Requirement and need wraparound support to aid engagement. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement.')
     , ('2c331ab1-e350-461c-b1b5-21269e4ffeb6', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')
-- DNR London - Low, Medium & High
     , ('3749abc1-ca74-40fd-8132-5a37c11dba4e', 'b34846f0-176e-483b-92d9-6621dda855eb', 'Low complexity',
        'Person(s) on Probation who is highly motivated and has had sustained period of abstinence. Person(s) on Probation has successfully engaged in a period of treatment but requires support to sustain recovery. ')
     , ('4ea7bd57-a189-4adb-8728-52eeeb16af8e', 'b34846f0-176e-483b-92d9-6621dda855eb', 'Medium complexity',
        'Person(s) on Probations who has had lapses/crises that may lead to relapse and/or is at a transition point, e.g. from prison into community. Person(s) on Probation has a moderate level of motivation but requires a high level of support for dependencies linked to re-offending. Person(s) on Probations who are subject to a Community Sentence Treatment Requirement and need wraparound support to aid engagement. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement.')
     , ('c3ee40ad-ad87-491a-8bc7-6598b6a86fa8', 'b34846f0-176e-483b-92d9-6621dda855eb', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')
-- DNR North East - High
     , ('e957a60a-3233-4972-8422-219b0bfb62a9', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')
-- DNR Kent, Surrey and Sussex - Medium & High
     , ('c09c0ed4-7c87-4484-a257-cb2607e95e12', '4adf3bd1-be2a-44dc-922d-8a915367b77c', 'Medium complexity',
        'Person(s) on Probations who has had lapses/crises that may lead to relapse and/or is at a transition point, e.g. from prison into community. Person(s) on Probation has a moderate level of motivation but requires a high level of support for dependencies linked to re-offending. Person(s) on Probations who are subject to a Community Sentence Treatment Requirement and need wraparound support to aid engagement. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement.')
     , ('2a7e49fa-cb1a-4f39-923f-f3dce922e758', '4adf3bd1-be2a-44dc-922d-8a915367b77c', 'High complexity',
        'Person(s) on Probations may include those with chronic substance misuse issues, those with additional needs which might include dual diagnosis and/or a chaotic lifestyle linked to their dependency. Person(s) on Probation does not meet the threshold for secondary provision treatment services and/or are deemed unsuitable following assessment for treatment as part of a Community Sentence Treatment Requirement. However, Person(s) on Probation has identifiable substance misuse, alcohol misuse, prescription medication misuse, or other dependencies linked to re-offending. Person(s) on Probation has a history of poor engagement with treatment services and requires a high level of motivation and support to engage with treatment.')

;

INSERT INTO desired_outcome (id, service_category_id, description)
-- DNR Yorkshire and the Humber
VALUES ('6695be52-7230-4ee8-924b-4ce811f687c2', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('1b90c7f2-6cb2-44b0-b317-ec7c8f9fc44b', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a',
        'Service user improves their physical health and mental resilience.')
     , ('c5213107-540f-4685-a08f-b3a0fb300030', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('29b64d0b-5778-4a4f-a743-2dc78cadc1c0', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('587c5ab7-eeb1-465e-a4be-4815b93c6bfd', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('39bfa839-efb4-474a-97dc-3c7fe23099a9', 'ca13b995-4c91-4d0a-9fe8-2055cc0acb1a',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
-- DNR London
     , ('c7e3309e-35a5-4f9f-8658-a9fb84667435', 'b34846f0-176e-483b-92d9-6621dda855eb',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('a50a7ab9-6812-4b25-9a5e-790b2cd37fcf', 'b34846f0-176e-483b-92d9-6621dda855eb',
        'Service user improves their physical health and mental resilience.')
     , ('e0a85ac2-ae2a-46c9-b45c-72aa960ebd7e', 'b34846f0-176e-483b-92d9-6621dda855eb',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('589c6fe5-455f-48a9-8305-d992fc96f8da', 'b34846f0-176e-483b-92d9-6621dda855eb',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('9dd34514-c0c2-4f00-9ec2-e59afe6ac2d9', 'b34846f0-176e-483b-92d9-6621dda855eb',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('2fd7de2c-8746-4a4d-9d09-2242c8959858', 'b34846f0-176e-483b-92d9-6621dda855eb',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
     , ('f7c8cd72-3d83-4dfc-9349-13895e9545ae', 'b34846f0-176e-483b-92d9-6621dda855eb',
        'Service user has continuity of care from prison into the community where there is no other available resource or commissioned service.')
-- DNR North East
     , ('d5777eb0-bdd4-4aab-88c4-db62d9c06dcc', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('5f671fcb-cfba-49e3-92db-09b9b443a973', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240',
        'Service user improves their physical health and mental resilience.')
     , ('e935da84-ecbc-4c51-976a-04de1f9ddacc', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('2df12ce0-17cc-40f4-a6d9-7b71376aced1', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('5696c601-a45c-48b3-99d2-3b6ea7cb4559', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('cd95d8a6-8d7d-4b9d-9546-6d22b47780ef', '3c7ad8ff-86d9-4ba9-87c6-dc2d7b741240',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
-- DNR Kent, Surrey and Sussex
     , ('4b23cac5-fb44-42e0-a9ea-dd220697e1f6', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user achieves abstinence or controlled/ non-dependent or non-problematic substance misuse.')
     , ('3fa5d3ee-3072-417d-89f3-06e1751d3c3e', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user improves their physical health and mental resilience.')
     , ('ef3b98e6-ea1b-45f3-bff3-7bef23a9bfdf', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user enhances their skills to manage risky situations which may pose a trigger or relapse.')
     , ('03da060f-7559-4c73-a6d9-284e7503feb0', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user enhances their belief in ability to manage/ desist from addiction(s).')
     , ('5b68b729-4820-4551-b59a-aea084518b69', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user increases understanding of addictive behaviours and triggers and options to reduce dependency.')
     , ('95fefcab-8bc7-43de-ade0-31b3e823b269', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user establishes Dependency Pathways to manage a range of addictive behaviours, including supporting access into other treatment providers and detox programmes.')
     , ('a8f3ed4f-d29a-45ff-8b83-3b0e16c620b9', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user has continuity of care from prison into the community where there is no other available resource or commissioned service.')
     , ('7e98f2c1-8f9a-4077-9c68-bb0a8df2dae4', '4adf3bd1-be2a-44dc-922d-8a915367b77c',
        'Service user has a minimised risk of overdose following release from prison.')
;
