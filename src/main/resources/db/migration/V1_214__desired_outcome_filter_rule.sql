CREATE TYPE rule_type AS ENUM ('EXCLUDE','INCLUDE');

create table desired_outcome_filter_rule(
                                id uuid not null,
                                desired_outcome_id uuid not null,
                                rule_type rule_type not null,
                                match_type text not null,
                                match_data text not null,

                                primary key (id),
                                constraint fk__desired_outcome_filter_rule__desired_outcome foreign key(desired_outcome_id) references desired_outcome
);