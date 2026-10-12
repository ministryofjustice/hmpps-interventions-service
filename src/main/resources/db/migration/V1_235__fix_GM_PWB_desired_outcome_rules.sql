-- be2cb69b-389c-457e-87d8-d756826abd82 and 32f14f09-7928-43da-8ac1-3b8f7e2fa307 should be available to all contracts,
-- so remove the rules added for Greater Manchester PWB contracts
DELETE FROM desired_outcome_filter_rule_match_data
WHERE desired_outcome_filter_rule_id IN ('e6b88496-840f-4f46-8357-d2a9df99c01f',
                                         '8919ce4e-f369-4e2c-a029-d34bf0ff7c8c');
DELETE FROM desired_outcome_filter_rule
WHERE id IN ('e6b88496-840f-4f46-8357-d2a9df99c01f',
             '8919ce4e-f369-4e2c-a029-d34bf0ff7c8c');

-- 73732960-b654-4856-a32c-a12d9511a17b should only be available to its existing INCLUDE rule (281bff3c-cf1a-4c33-bb81-3d219dd0dd9f),
-- so move the Greater Manchester PWB contracts back onto it and remove the duplicate rule
INSERT INTO desired_outcome_filter_rule_match_data (desired_outcome_filter_rule_id, match_data)
SELECT '281bff3c-cf1a-4c33-bb81-3d219dd0dd9f', match_data
FROM desired_outcome_filter_rule_match_data
WHERE desired_outcome_filter_rule_id = '5a5318b9-5704-486c-ae6e-ef240ed95dcb';

DELETE FROM desired_outcome_filter_rule_match_data WHERE desired_outcome_filter_rule_id = '5a5318b9-5704-486c-ae6e-ef240ed95dcb';
DELETE FROM desired_outcome_filter_rule WHERE id = '5a5318b9-5704-486c-ae6e-ef240ed95dcb';

-- New Greater Manchester PWB desired outcomes are already limited by their INCLUDE rules,
-- so remove the redundant PRJ EXCLUDE rules (only one rule per desired outcome is evaluated)
DELETE FROM desired_outcome_filter_rule_match_data
WHERE desired_outcome_filter_rule_id IN ('05664d4c-d6c4-4250-8d9f-1085a0cdddef',
                                         'b184ef11-db04-494f-a22c-409c33b14ded',
                                         '6165cae9-ae06-429e-b5f7-de6156f211fb');
DELETE FROM desired_outcome_filter_rule
WHERE id IN ('05664d4c-d6c4-4250-8d9f-1085a0cdddef',
             'b184ef11-db04-494f-a22c-409c33b14ded',
             '6165cae9-ae06-429e-b5f7-de6156f211fb');
