alter table draft_referral
add column ndelius_pp_telephone_number text null,
add column ndelius_pp_team_telephone_number text null,
add column pp_telephone_number text null,
add column pp_team_telephone_number text null;

alter table probation_practitioner_details
add column ndelius_pp_telephone_number text null,
add column ndelius_pp_team_telephone_number text null,
add column pp_telephone_number text null,
add column pp_team_telephone_number text null;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','ndelius_pp_telephone_number',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','ndelius_pp_team_telephone_number',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','pp_telephone_number',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','pp_team_telephone_number',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','ndelius_pp_telephone_number',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','ndelius_pp_team_telephone_number',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','pp_telephone_number',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','pp_team_telephone_number',TRUE, TRUE);

