INSERT INTO contract_type (id, name, code)
VALUES ('ee20282e-aea4-429e-8b3f-0d868c46e8bc', 'Finance, Benefit and Debt (Yorkshire and the Humber)', 'FBD-Y')
;

INSERT INTO service_category (id, created, name)
VALUES ('6c1541c1-13c4-4cc1-b7d6-d59671dd6954', NOW(), 'Finance, Benefit and Debt (Yorkshire and the Humber)')
;

INSERT INTO contract_type_service_category (contract_type_id, service_category_id)
VALUES ('ee20282e-aea4-429e-8b3f-0d868c46e8bc', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954')-- D&R West Midlands
; 

INSERT INTO complexity_level (id, service_category_id, title, description)
-- PRJ_7622 - FBD Yorkshire and Humber - Low, Medium & High
VALUES ('3e5e837f-7dfc-4a8d-b5ac-4c0b8ec92d68', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954', 'Low complexity',
'Service user has prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances may be currently leading to significant financial pressure. Service user will require some advice, guidance and/or support across some specific but limited finance, benefit and debt needs, but will be able to support themselves with other aspects.')
     , ('23ee19a2-cab8-4463-a258-4d11ac8dd5c6', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954', 'Medium complexity',
        'Service user has limited prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances are currently leading to significant financial pressure and they require support to address these needs. Service user will need either significant support to meet a limited number of needs – for instance the Service user may have additional needs (i.e. learning difficulties) or other challenges which impact on their ability to fully manage – or they will need limited support to meet a wider range of finance, benefit and debt - related needs.')
     , ('d25eb3de-6675-4fa6-b95f-e35168b71f3c', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954', 'High complexity',
        'Service user has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account. Service users circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs. Service user may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.')
;

INSERT INTO desired_outcome (id, service_category_id, description)
-- PRJ_7622 - FBD Yorkshire and Humber
VALUES ('c79d1449-6032-4570-adc9-abd39a54030d', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954',
'Service user has access to appropriate financial products, advice and/or services.')
, ('711e87e5-8b26-4f76-b047-181d915c8332', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954',
'Pathways are established to help service user maintain and sustain an income, safely manage money and reduce debt.')
, ('fc35b932-3bab-4c9d-89cd-b43dcf00a925', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954',
'Service user on probation financial management skills are developed and/or enhanced including online banking.')
, ('c8e5e3e7-743b-4783-9961-7fc7845d77d0', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954',
'Service user can successfully navigate the benefits system.')
, ('22a9ed05-3396-4ee8-bfad-61c16c824bc7', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954',
'Service user gains quick access to universal credit.')
, ('675bc288-dc86-4036-b1b8-1827f9137a2a', '6c1541c1-13c4-4cc1-b7d6-d59671dd6954',
'Service user’s financial management skills are developed and/or enhanced, including online banking skills.')
;
