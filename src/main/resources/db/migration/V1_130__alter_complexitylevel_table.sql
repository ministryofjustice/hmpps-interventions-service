CREATE TYPE complexities AS ENUM ('LOW','MEDIUM', 'HIGH');

alter table complexity_level add column complexity complexities;

update complexity_level set complexity = 'LOW' where title = 'Low complexity';
update complexity_level set complexity = 'MEDIUM' where title = 'Medium complexity';
update complexity_level set complexity = 'HIGH' where title = 'High complexity';

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('complexity_level','complexity',FALSE, TRUE);