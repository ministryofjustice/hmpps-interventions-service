create table withdrawal_reason (code varchar(3) not null, description text not null, primary key (code));

alter table referral
    add column withdrawal_reason_code varchar(3),
    add constraint FK_withdrawal_reason_code foreign key (withdrawal_reason_code) references withdrawal_reason;
