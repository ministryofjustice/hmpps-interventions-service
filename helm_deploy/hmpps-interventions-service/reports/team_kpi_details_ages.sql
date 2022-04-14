-- referral ages by contract type and NPS region
SELECT ct.name                                                                                          AS contract_type
     , nr.name                                                                                          AS probation_region
     , TO_CHAR(PERCENTILE_CONT(0.5) WITHIN GROUP ( ORDER BY DATE_TRUNC('day', NOW()) - r.created_at)
               FILTER ( WHERE r.sent_at ISNULL ), 'DD" days" HH24" hours"')                             AS draft_age_median
     , TO_CHAR(PERCENTILE_CONT(0.5) WITHIN GROUP ( ORDER BY DATE_TRUNC('day', NOW()) - r.sent_at)
               FILTER ( WHERE r.sent_at NOTNULL AND r.concluded_at ISNULL ), 'DD" days" HH24" hours"')  AS in_progress_age_median
     , TO_CHAR(PERCENTILE_CONT(0.5) WITHIN GROUP ( ORDER BY r.concluded_at - r.sent_at)
               FILTER ( WHERE r.sent_at NOTNULL AND r.concluded_at NOTNULL ), 'DD" days" HH24" hours"') AS finished_age_median
FROM referral r
         JOIN intervention i ON r.intervention_id = i.id
         JOIN dynamic_framework_contract c ON i.dynamic_framework_contract_id = c.id
         JOIN contract_type ct ON c.contract_type_id = ct.id
         LEFT JOIN pcc_region pr ON c.pcc_region_id = pr.id
         LEFT JOIN nps_region nr ON (c.nps_region_id = nr.id OR pr.nps_region_id = nr.id)
GROUP BY ct.name, nr.name
ORDER BY ct.name, nr.name;
