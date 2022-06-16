-- below columns were fully migrated to `referral_details` table, but deprecation renaming did not happen
ALTER TABLE referral
    DROP COLUMN maximum_enforceable_days,
    DROP COLUMN completion_deadline,
    DROP COLUMN further_information;

DELETE
FROM metadata
WHERE table_name = 'referral'
  AND column_name IN ('maximum_enforceable_days',
                      'completion_deadline',
                      'further_information');
