alter table referral_location
    add column nomis_prison_id text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_prison_id',TRUE, TRUE);
