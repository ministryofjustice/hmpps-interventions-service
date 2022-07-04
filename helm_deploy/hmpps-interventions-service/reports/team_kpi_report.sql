WITH counts AS (SELECT COUNT(r.id)                                                      AS count_of_started_referrals,
                       COUNT(r.id) FILTER ( WHERE r.sent_at IS NULL )                   AS count_of_draft_referrals,
                       COUNT(r.id) FILTER ( WHERE r.sent_at IS NOT NULL )               AS count_of_sent_referrals,
                       COUNT(r.id) FILTER ( WHERE r.concluded_at IS NOT NULL )          AS count_of_concluded_referrals,
                       COUNT(r.id) FILTER ( WHERE r.sent_at IS NOT NULL AND
                                                  r.concluded_at IS NULL )              AS count_of_live_referrals,
                       COUNT(r.id) FILTER ( WHERE r.concluded_at IS NOT NULL AND
                                                  r.end_requested_at IS NOT NULL AND
                                                  eosr.submitted_at IS NULL )           AS count_of_cancelled_referrals,
                       COUNT(r.id) FILTER ( WHERE r.concluded_at IS NOT NULL AND
                                                  r.end_requested_at IS NOT NULL AND
                                                  eosr.submitted_at IS NOT NULL )       AS count_of_early_end_referrals,
                       COUNT(r.id) FILTER ( WHERE r.end_requested_reason_code = 'MIS' ) AS count_of_mistaken_referrals
                FROM referral r
                         LEFT JOIN end_of_service_report eosr ON r.id = eosr.referral_id),

     times AS (SELECT PERCENTILE_CONT(0.50) WITHIN GROUP ( ORDER BY sent_at - created_at ) AS p50_completion_time,
                      PERCENTILE_CONT(0.95) WITHIN GROUP ( ORDER BY sent_at - created_at ) AS p95_completion_time,
                      PERCENTILE_CONT(0.99) WITHIN GROUP ( ORDER BY sent_at - created_at ) AS p99_completion_time
               FROM referral
               WHERE sent_at IS NOT NULL)

SELECT counts.count_of_started_referrals,
       counts.count_of_draft_referrals,
       counts.count_of_sent_referrals,
       counts.count_of_concluded_referrals,
       counts.count_of_live_referrals,
       counts.count_of_cancelled_referrals,
       counts.count_of_mistaken_referrals,
       counts.count_of_early_end_referrals,
       times.p50_completion_time,
       times.p95_completion_time,
       times.p99_completion_time

FROM counts,
     times;
