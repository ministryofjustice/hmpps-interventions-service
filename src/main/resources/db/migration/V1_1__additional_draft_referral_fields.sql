alter table referral
    alter column completion_deadline type date,
    add column caring_or_employment_responsibilities text,
    add column disabilities text,
    add column ethnicity text,
    add column gender text,
    add column accessibility_needs text,
    add column needs text,
    add column relegion_or_beliefs text,
    add column complexity_level text,
    add column sexual_orientation text,
    add column address text,
    add column crn_number text,
    add column dob date;
