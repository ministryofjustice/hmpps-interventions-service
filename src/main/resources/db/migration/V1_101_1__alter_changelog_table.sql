CREATE TYPE amendTypes AS ENUM ('COMPLEXITY_LEVEL','DESIRED_OUTCOMES');

alter table changelog alter column topic TYPE amendTypes using topic::amendTypes;