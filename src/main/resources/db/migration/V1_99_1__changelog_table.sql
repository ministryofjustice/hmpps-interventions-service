CREATE TABLE changelog(
                                 referral_id uuid not null,
                                 changelog_id uuid unique,
                                 topic text not null,
                                 old_value jsonb not null,
                                 new_value jsonb not null,
                                 reason_for_change text not null,
                                 changed_at timestamp with time zone not null,
                                 changed_by_id text not null,

                                 primary key(changelog_id),
                                 foreign key (referral_id) references referral(id),
                                 foreign key (changed_by_id) references auth_user(id)
);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','referral_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','changelog_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','topic',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','old_value',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','new_value',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','reason_for_change',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','changed_at',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('changelog','changed_by_id',FALSE, TRUE);
