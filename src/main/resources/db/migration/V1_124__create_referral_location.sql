CREATE TABLE referral_location (
    referral_id uuid not null,
    type person_current_location_type not null,
    prison_id text,
    expected_release_date date,
    expected_release_date_missing_reason text,

    constraint fk_referral_location_referral_id foreign key (referral_id) references referral
);

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','referral_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','prison_id',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','expected_release_date',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','expected_release_date_missing_reason',TRUE, TRUE);