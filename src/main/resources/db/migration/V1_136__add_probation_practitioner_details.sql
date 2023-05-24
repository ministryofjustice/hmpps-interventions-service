alter table draft_referral
    add column ndelius_pp_name text,
    add column ndelius_pp_email_address text,
    add column ndelius_pp_pdu text,
    add column pp_name text,
    add column pp_email_address text,
    add column pp_pdu text,
    add column pp_probation_office text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','ndelius_pp_name',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','ndelius_pp_email_address',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','ndelius_pp_pdu',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','pp_name',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','pp_email_address',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','pp_pdu',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','pp_probation_office',TRUE, TRUE);