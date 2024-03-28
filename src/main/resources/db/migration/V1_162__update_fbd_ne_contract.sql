update intervention
set title = 'Finance, Benefit & Debt in the North East',
    description = 'Services are available for all adult males on probation either on a Community Sentence or Suspended Sentence Order and subject to a Rehabilitation Activity Requirement (RAR) whereby a specified number of RAR Activity Days will be allocated, pre-release to people who are sentenced released on licence and post-sentence supervision. Services are also available to all adult males pre-release who are sentenced.

These services can be delivered at any point during the custodial element of the sentence and can be accessed by all people sentenced, regardless of the location they will be released to. Interventions commenced in custody, but not completed prior to release, may continue in the community until the intervention is completed under the same referral.

Services can be delivered as part of a Rehabilitation Activity Requirement (RAR) whereby a specified number of RAR Activity Days will be allocated by the Probation Service for those subject to a Community Sentence or Suspended Sentence Order. Any Sessions delivered as part of a RAR Activity Day or as a mandated Licence or Post Sentence Supervision appointment will be Enforceable.

Services are offered as Medium and High complexity for Community cases

Services are offered High complexity for custody cases

A range of delivery methods need to be used including: designing and delivery, support and advocacy and advice, guidance and information.

Interventions must be tailored to meet the specific needs of each Person(s) on Probation to enable them to make progress towards their agreed outcomes, as identified in the Person(s) on Probations Action Plan and aim to secure one or more of the following outcomes:

• the person referred’s financial management skills are developed and/or enhanced, including online banking skills
• the person referred can successfully navigate the benefits system
• Pathways are established to help the person referred to maintain and sustain an income, safely manage money and reduce debt
• the person referred has access to appropriate financial products, advice and/or services
• the person referred gains quick access to universal credit, including pre-release referrals
• the person referred is supported to complete tasks that they would not otherwise be able to do whilst being in custody e.g. support with banking or debt management

Activities:

Finance Benefits & Debt
1:1; HMP Durham HMP Holme House HMP Northumberland HMP Deerbolt HMP Kirklevington Grange HMP Frankland / Cleveland - Stockton & Hartlepool / Redcar, Cleveland and Middlesbrough / Durham - County Durham and Darlington / Northumbria - South Tyneside & Gateshead / Northumberland / Newcastle / Sunderland
• Access to advice
• Access to benefits
• Benefits
• Custody Support
• Financial Management
• Money Management'
where id = (select id from intervention where dynamic_framework_contract_id = (select id from dynamic_framework_contract where contract_reference = 'PRJ_7647'));

update service_category set name = 'Finance, benefit and debt in the North East' where name = 'Finance, Benefit and Debt in Custody (North East)';

insert into complexity_level (id, title, description, service_category_id, complexity)
values ('02c7faed-2263-4f69-9ade-0377bd28d2a1',
        'Medium complexity',
        'Person(s) on Probation has limited prior experience of successfully dealing with their own finance, benefit and debt needs. Person(s) on Probations circumstances are currently leading to significant financial pressure and they require support to address these needs. Person(s) on Probation will need either significant support to meet a limited number of needs – for instance the Person(s) on Probation may have additional needs (i.e. learning difficulties) or other challenges which impact on their ability to fully manage – or they will need limited support to meet a wider range of finance, benefit and debt - related needs.',
        (select id from service_category where name = 'Finance, benefit and debt in the North East'),
        'MEDIUM');