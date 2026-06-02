-- draft_referral was created via 'CREATE TABLE AS SELECT * FROM referral' (V1_108),
-- which copies rows but NOT indexes. The referral table has IX_referral_service_usercrn
-- (V1_57), but draft_referral never got the equivalent index.
-- Without this index the findDraftOnlyByServiceUserCRN query does a full table scan.
create index concurrently if not exists IX_draft_referral_service_usercrn
    on draft_referral (service_usercrn);
