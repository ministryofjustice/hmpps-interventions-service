CREATE TYPE person_current_location_type AS ENUM ('CUSTODY','COMMUNITY');

alter table draft_referral
    add column person_current_location_type person_current_location_type,
    add column person_custody_prison_id text;