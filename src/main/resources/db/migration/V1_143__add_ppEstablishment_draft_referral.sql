alter table draft_referral
    add column pp_establishment text;

alter table probation_practitioner_details
    add column establishment text;


INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','pp_establishment',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','establishment',TRUE, TRUE);