alter table dynamic_framework_contract
    add column referral_start_date date not null default '2021-01-01';

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('dynamic_framework_contract','referral_start_date',FALSE, TRUE);
