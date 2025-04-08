CREATE TYPE rule_type AS ENUM ('EXCLUDE','INCLUDE');

create table desired_outcome_filter_rule(
                                id uuid not null,
                                desired_outcome_id uuid not null,
                                rule_type rule_type not null,
                                match_type text not null,

                                primary key (id),
                                constraint fk__desired_outcome_filter_rule__desired_outcome foreign key(desired_outcome_id) references desired_outcome
);

create table desired_outcome_filter_rule_match_data(
                                 desired_outcome_filter_rule_id uuid not null,
                                 match_data text not null,

                                 unique(desired_outcome_filter_rule_id , match_data),
                                 constraint fk__referral_desired_outcome__referral foreign key (desired_outcome_filter_rule_id) references desired_outcome_filter_rule
);

COMMENT ON TABLE desired_outcome_filter_rule IS 'stores rules about when a desired outcome should be filtered out.';
COMMENT ON TABLE desired_outcome_filter_rule_match_data IS 'stored the data that should be matched against when filtering.';

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('desired_outcome_filter_rule','id', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('desired_outcome_filter_rule','desired_outcome_id', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('desired_outcome_filter_rule','rule_type', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('desired_outcome_filter_rule','match_type', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('desired_outcome_filter_rule_match_data','desired_outcome_filter_rule_id', FALSE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('desired_outcome_filter_rule_match_data','match_data', FALSE, TRUE);
