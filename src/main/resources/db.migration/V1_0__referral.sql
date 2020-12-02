CREATE TABLE IF NOT EXISTS referral
(
    referralId       SERIAL          PRIMARY KEY,
    referralUuid     UUID            UNIQUE,
    completeByDate   TIMESTAMP,
    createdDate      TIMESTAMP
);