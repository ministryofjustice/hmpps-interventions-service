alter table referral_location
    add column nomis_release_date date,
    add column nomis_confirmed_release_date date,
    add column nomis_non_dto_release_date date,
    add column nomis_automatic_release_date date,
    add column nomis_post_recall_release_date date,
    add column nomis_conditional_release_date date,
    add column nomis_actual_parole_date date,
    add column nomis_discharge_date date;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_confirmed_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_non_dto_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_automatic_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_post_recall_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_conditional_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_actual_parole_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','nomis_discharge_date',TRUE, TRUE);
