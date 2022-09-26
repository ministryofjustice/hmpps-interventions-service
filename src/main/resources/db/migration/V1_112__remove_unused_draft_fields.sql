-- all the columns dropped here have latest data from 25 August 2022; so they haven't been set for a month,
-- which proves they are not used for writing
ALTER TABLE draft_referral
    -- below: already deprecated
    DROP COLUMN deprecated_assigned_at,
    DROP COLUMN deprecated_assigned_by_id,
    DROP COLUMN deprecated_assigned_to_id,
    -- below: draft referrals cannot be cancelled/ended early/finished
    DROP COLUMN end_requested_at,
    DROP COLUMN end_requested_by_id,
    DROP COLUMN end_requested_reason_code,
    DROP COLUMN end_requested_comments,
    DROP COLUMN concluded_at,
    -- below: written during "send", so not required for drafts
    DROP COLUMN supplementary_risk_id,
    DROP COLUMN sent_at,
    DROP COLUMN sent_by_id;

DELETE
FROM metadata
WHERE table_name = 'draft_referral'
  AND column_name IN ('deprecated_assigned_at',
                      'deprecated_assigned_by_id',
                      'deprecated_assigned_to_id',
                      'end_requested_at',
                      'end_requested_by_id',
                      'end_requested_reason_code',
                      'end_requested_comments',
                      'concluded_at',
                      'supplementary_risk_id',
                      'sent_at',
                      'sent_by_id');
