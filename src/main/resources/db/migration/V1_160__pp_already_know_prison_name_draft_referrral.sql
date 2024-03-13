alter table draft_referral
add column already_know_prison_name bool null;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','already_know_prison_name',TRUE, TRUE);
