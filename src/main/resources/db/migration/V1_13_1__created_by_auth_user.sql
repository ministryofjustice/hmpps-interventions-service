-- add the new column type that will replace both 'created_by_userid' and 'created_by_user_auth_source'
alter table referral
    add column created_by_id text,
    add constraint fk_created_by_id foreign key (created_by_id) references auth_user;

-- create any auth user entities that exist in the referral table but not the auth_user table.
insert into auth_user (id, auth_source)
    select created_by_userid, created_by_user_auth_source
    from referral
    on conflict do nothing;

-- copy over all 'created_by_userid' values to the 'created_by_id' column. these columns hold the same value.
update referral
    set created_by_id = created_by_userid;

alter table referral
    alter column created_by_id set not null;

-- drop the index on the old 'created_by_userid' column and add it to the new 'created_by_id' column.
drop index IX_referral_created_by_userid;
create index IX_referral_created_by_id on referral (created_by_id);
create index IX_referral_sent_by_id on referral (sent_by_id);

-- finally, drop the old user columns.
alter table referral
    drop column created_by_userid,
    drop column created_by_user_auth_source;
