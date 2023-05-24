CREATE TABLE probation_practitioner_details (
    id uuid not null,
    referral_id uuid not null,
    ndelius_name text,
    ndelius_email_address text,
    ndelius_pdu text,
    name text,
    email_address text,
    pdu text,
    probation_office text,
    primary key (id),
    constraint fk_probation_practitioner_details_referral_id foreign key (referral_id) references referral
);

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','referral_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','ndelius_name',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','ndelius_email_address',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','ndelius_pdu',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','name',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','email_address',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','pdu',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('probation_practitioner_details','probation_office',TRUE, TRUE);
