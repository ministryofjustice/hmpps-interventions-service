alter table draft_referral
add column expected_probation_office text null,
add column expected_probation_office_unknown_reason text null;

alter table referral_location
add column expected_probation_office text null,
add column expected_probation_office_unknown_reason text null;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','expected_probation_office',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','expected_probation_office',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','expected_probation_office_unknown_reason',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','expected_probation_office_unknown_reason',TRUE, TRUE);
