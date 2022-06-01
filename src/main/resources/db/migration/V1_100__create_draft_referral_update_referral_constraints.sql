INSERT INTO metadata (table_name, column_name, sensitive, domain_data)
  select 'draft_referral',column_name,sensitive,domain_data from metadata  where table_name='referral';

CREATE TABLE draft_referral AS SELECT * FROM referral;

ALTER TABLE draft_referral ADD PRIMARY KEY (id);

alter table referral_service_user_data
DROP CONSTRAINT fk_referral_id;

alter table referral_desired_outcome
DROP CONSTRAINT fk__referral_desired_outcome__referral;

alter table referral_complexity_level_ids
DROP CONSTRAINT fk_referral_complexity_level_ids_referral_id;

alter table draft_oasys_risk_information
DROP CONSTRAINT fk_draft_oasys_risk_information_referral_id;

alter table referral_details
DROP CONSTRAINT fk_referral_details_referral_id;

alter table referral_selected_service_category
DROP CONSTRAINT fk_referral_id;

alter table referral_service_user_data
add constraint fk_referral_id foreign key (referral_id) references draft_referral;

alter table referral_desired_outcome
add constraint fk__referral_desired_outcome__referral foreign key (referral_id) references draft_referral;

alter table referral_complexity_level_ids
add constraint fk_referral_complexity_level_ids_referral_id foreign key (referral_id) references draft_referral;

alter table draft_oasys_risk_information
add constraint fk_draft_oasys_risk_information_referral_id foreign key (referral_id) references draft_referral;

alter table referral_details
add constraint fk_referral_details_referral_id foreign key (referral_id) references draft_referral;

alter table referral_selected_service_category
add constraint fk_referral_id foreign key (referral_id) references draft_referral;