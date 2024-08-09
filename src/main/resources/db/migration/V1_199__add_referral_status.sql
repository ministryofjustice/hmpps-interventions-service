CREATE TYPE status AS ENUM ('PRE_ICA','POST_ICA');

alter table referral
    add column status status;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral','status', FALSE, TRUE);