alter table draft_referral
    add column person_expected_release_date date,
    add column person_expected_release_date_missing_reason text;

alter table referral_location
    add column expected_release_date date,
    add column expected_release_date_missing_reason text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','person_expected_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','person_expected_release_date_missing_reason',TRUE, TRUE);

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','expected_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','expected_release_date_missing_reason',TRUE, TRUE);