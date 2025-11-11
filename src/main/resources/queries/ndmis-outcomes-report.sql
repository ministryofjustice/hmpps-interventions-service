SELECT
    r.reference_number AS referral_ref,
    r.id AS referral_id,
    deso.description AS desired_outcome_description,
    esor.achievement_level AS achievement_level
FROM referral r
         INNER JOIN end_of_service_report eosr ON r.id = eosr.referral_id
         INNER JOIN end_of_service_report_outcome esor ON eosr.id = esor.end_of_service_report_id
         INNER JOIN desired_outcome deso ON esor.desired_outcome_id = deso.id
ORDER BY r.id