create table dynamic_framework_contract_pcc_regions (
    dynamic_framework_contract_id uuid not null,
    pcc_regions_id text not null,
    primary key (dynamic_framework_contract_id, pcc_regions_id)
);

-- populate the join table with existing relationships
insert into dynamic_framework_contract_pcc_regions (dynamic_framework_contract_id, pcc_regions_id)
    select id, pcc_region_id from dynamic_framework_contract
    where pcc_region_id is not null;

alter table dynamic_framework_contract
    drop column pcc_region_id;
