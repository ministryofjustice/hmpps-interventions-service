CREATE TYPE complexities AS ENUM ('LOW_COMPLEXITY','MEDIUM_COMPLEXITY', 'HIGH_COMPLEXITY');

alter table complexity_level add column complexity_level_title complexities;

update complexity_level set complexity_level_title = 'LOW_COMPLEXITY' where title = 'Low complexity';
update complexity_level set complexity_level_title = 'MEDIUM_COMPLEXITY' where title = 'Medium complexity';
update complexity_level set complexity_level_title = 'HIGH_COMPLEXITY' where title = 'High complexity';

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('complexity_level','complexity_level_title',FALSE, TRUE);