-- referrals by contract type and NPS region
SELECT ct.name                                                                   AS contract_type
     , COUNT(r.id) FILTER ( WHERE r.sent_at ISNULL )                             AS draft
     , COUNT(r.id) FILTER ( WHERE r.sent_at NOTNULL )                            AS received
     , COUNT(r.id) FILTER ( WHERE r.sent_at NOTNULL AND r.concluded_at ISNULL )  AS in_progress
     , COUNT(r.id) FILTER ( WHERE r.sent_at NOTNULL AND r.concluded_at NOTNULL ) AS finished
     , ROUND(100.0 * COUNT(r.id) FILTER ( WHERE r.sent_at NOTNULL AND r.concluded_at ISNULL ) /
             COUNT(r.id) FILTER ( WHERE r.sent_at NOTNULL ), 2)                  AS in_progress_percent
FROM referral r
         JOIN intervention i ON r.intervention_id = i.id
         JOIN dynamic_framework_contract c ON i.dynamic_framework_contract_id = c.id
         JOIN contract_type ct ON c.contract_type_id = ct.id
GROUP BY 1
ORDER BY 1;
