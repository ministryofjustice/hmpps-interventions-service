ALTER TABLE draft_referral
    DROP COLUMN reference_number;

DELETE
FROM metadata
WHERE table_name = 'draft_referral'
  AND column_name = 'reference_number';
