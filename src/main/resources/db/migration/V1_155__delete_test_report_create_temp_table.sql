DROP TABLE test_reports;

alter table referral_location
drop column test_reports;

DELETE FROM metadata where table_name = 'referral_location' AND column_name = 'test_reports';
DELETE FROM metadata where table_name = 'test_reports' AND column_name = 'referral_id';
DELETE FROM metadata where table_name = 'test_reports' AND column_name = 'test_id';
DELETE FROM metadata where table_name = 'test_reports' AND column_name = 'topic';