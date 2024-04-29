create table withdrawal_reason (code varchar(3) not null, description text not null, grouping text not null, primary key (code));

alter table referral
    add column withdrawal_reason_code varchar(3),
    add column withdrawal_comments varchar(2500),
    add constraint FK_withdrawal_reason_code foreign key (withdrawal_reason_code) references withdrawal_reason;


insert into withdrawal_reason (code, description, grouping) values
                                                      ('INE','Ineligible Referral', 'problem'),
                                                      ('MIS','Mistaken or duplicate referral', 'problem'),

                                                      ('NOT','Not engaged', 'user'),
                                                      ('NEE','Needs met through another route', 'user'),
                                                      ('MOV','Moved out of service area', 'user'),
                                                      ('WOR','Work or caring responsibilities', 'user'),
                                                      ('USE','User died', 'user'),

                                                      ('ACQ','Acquitted on appeal', 'sentence'),
                                                      ('RET','Returned to custody', 'sentence'),
                                                      ('SER','Sentence revoked', 'sentence'),
                                                      ('SEE','Sentence expired', 'sentence'),

                                                      ('ANO','Another reason', 'other')
;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('withdrawal_reason','code', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('withdrawal_reason','description', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('withdrawal_reason','grouping', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral','withdrawal_reason_code', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral','withdrawal_comments', TRUE, TRUE);

