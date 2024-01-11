CREATE TABLE test_reports(
                                 referral_id uuid not null,
                                 test_id uuid unique,
                                 topic text null,
                                 primary key(test_id),
                                 foreign key (referral_id) references referral(id)
);
alter table referral_location
add column test_reports text null;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_location','test_reports',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('test_reports','referral_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('test_reports','test_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('test_reports','topic',FALSE, TRUE);