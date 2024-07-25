CREATE TABLE relevant_sentence_end_dataload (
    relevant_sentence_id bigint not null,
    relevant_sentence_end_date date not null,

    primary key (relevant_sentence_id)
);

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('relevant_sentence_end_dataload','relevant_sentence_id',FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('relevant_sentence_end_dataload','relevant_sentence_end_date',TRUE, TRUE);
