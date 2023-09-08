alter table draft_referral
    add column referral_releasing_12_weeks boolean,
    add column role_job_title text,
    add column has_main_point_of_contact_details boolean;


alter table referral_location
    add column referral_releasing_12_weeks boolean;

alter table probation_practitioner_details
    add column role_job_title text;


INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','referral_releasing_12_weeks',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','referral_releasing_12_weeks',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','role_job_title',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','has_main_point_of_contact_details',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','role_job_title',TRUE, TRUE);