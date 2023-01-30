CREATE TYPE person_current_location_type AS ENUM ('CUSTODY','COMMUNITY');

alter table draft_referral
    add column person_current_location_type person_current_location_type,
    add column person_custody_prison_id text;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','person_current_location_type',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','person_custody_prison_id',TRUE, TRUE);
