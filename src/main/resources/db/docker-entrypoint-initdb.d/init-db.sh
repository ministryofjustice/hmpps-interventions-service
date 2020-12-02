#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE interventions;

    CREATE TABLE IF NOT EXISTS referral
    (
        referralId       SERIAL          PRIMARY KEY,
        referralUuid     UUID            UNIQUE,
        completeByDate   TIMESTAMP,
        createdDate      TIMESTAMP
    );
EOSQL
