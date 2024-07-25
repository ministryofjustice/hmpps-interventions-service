alter table referral
add column relevant_sentence_end_date date null;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral','relevant_sentence_end_date',TRUE, TRUE);