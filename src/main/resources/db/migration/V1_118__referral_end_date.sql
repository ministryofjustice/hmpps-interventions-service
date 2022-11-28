alter table dynamic_framework_contract
    add column referral_end_at timestamp with time zone;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('dynamic_framework_contract','referral_end_at',FALSE, TRUE);
