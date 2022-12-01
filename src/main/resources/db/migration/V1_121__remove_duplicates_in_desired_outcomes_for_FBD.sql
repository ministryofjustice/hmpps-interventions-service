DELETE from referral_desired_outcome where desired_outcome_id = 'af1d6c9c-bcdd-4e9f-a778-5c1a1e90163c';

DELETE from desired_outcome where id in (
'af1d6c9c-bcdd-4e9f-a778-5c1a1e90163c',
'ff927255-28df-4a1d-8076-9ce18d959442',
'8ec86c5f-0788-446a-bc4b-108e1c83dc25',
'7058935b-cc4d-43a9-8bf5-a0ef4575d746',
'f869b53d-14d6-48bf-842f-f988ef845ee6',
'd8fb1e8d-43db-4a42-abda-0c64116ab441',
'86656efa-de29-4a58-b7d0-25c8ac1a15b4');

update desired_outcome set description = 'The person referred can successfully navigate the benefits system' where id = '39393e5f-48d2-4981-84d1-e23d762f65b8';
update desired_outcome set description = 'The person referred’s financial management skills are developed and/or enhanced, including online banking skills' where id = 'a0e18ea2-9982-43c5-9155-8a07ec88fe75';
