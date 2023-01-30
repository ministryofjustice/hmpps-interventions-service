CREATE TABLE referral_location (
    id uuid not null,
    referral_id uuid not null,
    type person_current_location_type not null,
    prison_id text,

    primary key (id),
    constraint fk_referral_location_referral_id foreign key (referral_id) references referral
);

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','referral_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','type',TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','prison_id',TRUE, TRUE);
