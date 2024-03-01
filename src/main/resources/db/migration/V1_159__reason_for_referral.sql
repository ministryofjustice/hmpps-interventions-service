alter table referral_details
add column reason_for_referral text null;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_details','reason_for_referral',TRUE, TRUE);

