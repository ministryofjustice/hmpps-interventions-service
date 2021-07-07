create table referral_nominated_access (
    referral_id uuid not null,
    subcontractor_provider_id varchar(30) not null,
    constraint fk_referral_nominated_access_referral_id foreign key(referral_id) references referral,
    constraint fk_referral_nominated_access_subcontractor_provider_id foreign key(subcontractor_provider_id) references service_provider
)
