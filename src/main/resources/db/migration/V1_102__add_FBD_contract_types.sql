INSERT INTO contract_type (id, name, code)
VALUES ('5aa47f40-5f98-4300-a274-f8272c2f127d', 'Finance, Benefit and Debt (East Midlands)', 'FBD-EM')
     , ('4c29a22e-39a5-41ef-a8e4-69c8507ae058', 'Finance, Benefit and Debt (West Midlands)', 'FBD-WM')
     , ('c15ad3f3-f24b-4cd6-8ff6-03df4927fbe9', 'Finance, Benefit and Debt (London)', 'FBD-L')
     , ('f7569c39-5c3a-457e-9a29-435858af55e0', 'Finance, Benefit and Debt (South Central)', 'FBD-SC') -- Hampshire / Thames Valley
;

INSERT INTO service_category (id, created, name)
VALUES ('f2b00cea-a4f7-4919-b2ab-d8b6fc970c80', NOW(), 'Finance, Benefit and Debt (East Midlands)')
     , ('93283d91-f87a-43d2-83a5-6a6a27f5965e', NOW(), 'Finance, Benefit and Debt (West Midlands)')
     , ('64bc7502-7cec-410d-af7d-d3bae65b47ad', NOW(), 'Finance, Benefit and Debt (London)')
     , ('1426a44a-bb12-4117-97cd-46afcbff7f3f', NOW(), 'Finance, Benefit and Debt (South Central)') -- Hampshire / Thames Valley
     ;

INSERT INTO contract_type_service_category (contract_type_id, service_category_id)
VALUES ('5aa47f40-5f98-4300-a274-f8272c2f127d', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80') -- East Midlands
     , ('4c29a22e-39a5-41ef-a8e4-69c8507ae058', '93283d91-f87a-43d2-83a5-6a6a27f5965e') -- West Midlands
     , ('c15ad3f3-f24b-4cd6-8ff6-03df4927fbe9', '64bc7502-7cec-410d-af7d-d3bae65b47ad') -- London
     , ('f7569c39-5c3a-457e-9a29-435858af55e0', '1426a44a-bb12-4117-97cd-46afcbff7f3f') -- South Central
; 

INSERT INTO complexity_level (id, service_category_id, title, description)
-- FBD East Midlands - Medium & High
VALUES ('05b7d88b-5055-4eac-b1d2-ee04f873ba96', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80', 'Medium complexity',
        'Service user has limited prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances are currently leading to significant financial pressure and they require support to address these needs. Service user will need either significant support to meet a limited number of needs – for instance the Service user may have additional needs (i.e. learning difficulties) or other challenges which impact on their ability to fully manage – or they will need limited support to meet a wider range of finance, benefit and debt - related needs.')
     , ('3401b945-5ca5-4ce4-8174-8e444d0f006e', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80', 'High complexity',
        'Service user has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account. Service users circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs. Service user may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.')
-- FBD West Midlands - Medium & High
     , ('fd909155-395c-4623-ae81-68c44a85f886', '93283d91-f87a-43d2-83a5-6a6a27f5965e', 'Medium complexity',
        'Service user has limited prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances are currently leading to significant financial pressure and they require support to address these needs. Service user will need either significant support to meet a limited number of needs – for instance the Service user may have additional needs (i.e. learning difficulties) or other challenges which impact on their ability to fully manage – or they will need limited support to meet a wider range of finance, benefit and debt - related needs.')
     , ('3fda317c-7bcb-4712-b8fb-c5dbff3929b1', '93283d91-f87a-43d2-83a5-6a6a27f5965e', 'High complexity',
        'Service user has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account. Service users circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs. Service user may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.')
-- FBD London - Low, Medium & High
     , ('394383cf-e62d-4742-99c1-752963d69c33', '64bc7502-7cec-410d-af7d-d3bae65b47ad', 'Low complexity',
        'Service user has prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances may be currently leading to significant financial pressure. Service user will require some advice, guidance and/or support across some specific but limited finance, benefit and debt needs, but will be able to support themselves with other aspects.')
     , ('d3facfda-5d61-4b12-9178-bd71eb0a578a', '64bc7502-7cec-410d-af7d-d3bae65b47ad', 'Medium complexity',
        'Service user has limited prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances are currently leading to significant financial pressure and they require support to address these needs. Service user will need either significant support to meet a limited number of needs – for instance the Service user may have additional needs (i.e. learning difficulties) or other challenges which impact on their ability to fully manage – or they will need limited support to meet a wider range of finance, benefit and debt - related needs.')
     , ('edd90d34-3c89-4306-92e5-6eb274ec645f', '64bc7502-7cec-410d-af7d-d3bae65b47ad', 'High complexity',
        'Service user has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account. Service users circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs. Service user may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.')
-- FBD South Central - Low, Medium & High
     , ('595f8998-60a8-4427-809d-5d797568e5ca', '1426a44a-bb12-4117-97cd-46afcbff7f3f', 'Low complexity',
        'Service user has prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances may be currently leading to significant financial pressure. Service user will require some advice, guidance and/or support across some specific but limited finance, benefit and debt needs, but will be able to support themselves with other aspects.')
     , ('95b76f07-45e6-4791-9bc8-3450d1d95da6', '1426a44a-bb12-4117-97cd-46afcbff7f3f', 'Medium complexity',
        'Service user has limited prior experience of successfully dealing with their own finance, benefit and debt needs. Service users circumstances are currently leading to significant financial pressure and they require support to address these needs. Service user will need either significant support to meet a limited number of needs – for instance the Service user may have additional needs (i.e. learning difficulties) or other challenges which impact on their ability to fully manage – or they will need limited support to meet a wider range of finance, benefit and debt - related needs.')
     , ('8f542d7d-8a17-4d55-abea-3a9c3b55e794', '1426a44a-bb12-4117-97cd-46afcbff7f3f', 'High complexity',
        'Service user has minimal or no prior experience of successfully dealing with their own finance, benefit and debt needs and does not have necessary identification documents or bank account. Service users circumstances are currently leading to extreme financial pressure and/or they have complex finance, benefit and debt-related needs which require a range of Activities to address these needs. Service user may have a poor finance history and wider complex needs, such as mental health issues, substance misuse and/or addiction issues.')
;

INSERT INTO desired_outcome (id, service_category_id, description)
-- FBD East Midlands
VALUES ('6bc1dc66-77c4-4dc6-b38a-21d5c3929822', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80',
        'Service user has access to appropriate financial products, advice and/or services.')
     , ('bb483995-efae-47f7-b33a-0a1ab453d543', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80',
        'Pathways are established to help service user maintain and sustain an income, safely manage money and reduce debt.')
     , ('92633936-af55-4e05-87ab-745b65fb75bc', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80',
        'Service user on probation financial management skills are developed and/or enhanced including online banking.')
     , ('c8fe5ad7-01a6-41a5-a3fb-950554e8c0f4', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80',
        'Service user can successfully navigate the benefits system.')
     , ('88ec4638-70c6-4e70-b543-bf7877ad5718', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80',
        'Service user gains quick access to universal credit.')
     , ('b16a54f3-fa51-4bcb-bc33-4df47cbdec79', 'f2b00cea-a4f7-4919-b2ab-d8b6fc970c80',
        'Service user’s financial management skills are developed and/or enhanced, including online banking skills.')
-- FBD West Midlands
     , ('cd457ebc-004d-4933-a7ee-747a1b231512', '93283d91-f87a-43d2-83a5-6a6a27f5965e',
        'Service user has access to appropriate financial products, advice and/or services.')
     , ('6ce29ec1-b568-482b-863b-bc43a81b8e30', '93283d91-f87a-43d2-83a5-6a6a27f5965e',
        'Pathways are established to help service user maintain and sustain an income, safely manage money and reduce debt.')
     , ('a5041cd8-7200-4f84-8c59-3c2990153ab8', '93283d91-f87a-43d2-83a5-6a6a27f5965e',
        'Service user on probation financial management skills are developed and/or enhanced including online banking.')
     , ('98f0c533-2c40-4d01-9913-2febadf4f7e4', '93283d91-f87a-43d2-83a5-6a6a27f5965e',
        'Service user can successfully navigate the benefits system.')
     , ('94bfc9b6-a65f-46db-8227-3685dfb18a09', '93283d91-f87a-43d2-83a5-6a6a27f5965e',
        'Service user gains quick access to universal credit.')
-- FBD London
     , ('cb40474b-a6fa-4fed-a7d2-bf6b40cbae64', '64bc7502-7cec-410d-af7d-d3bae65b47ad',
        'Service user has access to appropriate financial products, advice and/or services.')
     , ('fc1f5ddd-ff7e-4fb2-b1e2-2fd4ab47f7b3', '64bc7502-7cec-410d-af7d-d3bae65b47ad',
        'Pathways are established to help service user maintain and sustain an income, safely manage money and reduce debt.')
     , ('ae992142-dc45-4225-9c6a-078ca5dd3624', '64bc7502-7cec-410d-af7d-d3bae65b47ad',
        'Service user on probation financial management skills are developed and/or enhanced including online banking.')
     , ('38f41ce0-d0e4-4334-8b56-3eac1531bf18', '64bc7502-7cec-410d-af7d-d3bae65b47ad',
        'Service user can successfully navigate the benefits system.')
     , ('21e4ea2f-9aa6-4f3a-96ae-b278e99fd9db', '64bc7502-7cec-410d-af7d-d3bae65b47ad',
        'Service user gains quick access to universal credit.')
-- FBD South Central
     , ('947c597f-abcd-43bb-8499-8a6f2fdf6725', '1426a44a-bb12-4117-97cd-46afcbff7f3f',
        'Service user has access to appropriate financial products, advice and/or services.')
     , ('d825be6a-1f54-4b9a-ae03-1a110da8daa7', '1426a44a-bb12-4117-97cd-46afcbff7f3f',
        'Pathways are established to help service user maintain and sustain an income, safely manage money and reduce debt.')
     , ('101f2dd9-1e54-4b05-9ab0-732c2d4786ec', '1426a44a-bb12-4117-97cd-46afcbff7f3f',
        'Service user on probation financial management skills are developed and/or enhanced including online banking.')
     , ('da54ceca-50a7-4a22-8791-44b23ab00ad8', '1426a44a-bb12-4117-97cd-46afcbff7f3f',
        'Service user can successfully navigate the benefits system.')
     , ('6c7d0537-d855-4e87-9270-ea5cd6a136c8', '1426a44a-bb12-4117-97cd-46afcbff7f3f',
        'Service user gains quick access to universal credit.')
;
