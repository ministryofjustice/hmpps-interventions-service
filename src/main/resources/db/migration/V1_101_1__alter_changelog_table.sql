CREATE TYPE amendTypes AS ENUM ('COMPLEXITY_LEVEL',
                                'DESIRED_OUTCOMES',
                                'COMPLETION_DATETIME',
                                'MAXIMUM_ENFORCEABLE_DAYS');

alter table changelog alter column topic TYPE amendTypes using topic::amendTypes;