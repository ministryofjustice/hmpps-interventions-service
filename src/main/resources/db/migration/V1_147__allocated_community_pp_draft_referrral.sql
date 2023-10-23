alter table draft_referral
add column allocated_community_pp boolean;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('draft_referral','allocated_community_pp',TRUE, TRUE);