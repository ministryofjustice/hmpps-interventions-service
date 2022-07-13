WITH dates AS (SELECT (DATE_TRUNC('day', NOW()) - '7 days'::interval) AS start_date,
                      DATE_TRUNC('day', NOW())                        AS end_date),

     counts AS (SELECT COUNT(r.id)                                                AS count_of_started_referrals,
                       COUNT(r.id) FILTER ( WHERE r.sent_at IS NULL )             AS count_of_draft_referrals,
                       COUNT(r.id) FILTER ( WHERE r.sent_at IS NOT NULL )         AS count_of_sent_referrals,
                       COUNT(r.id) FILTER ( WHERE r.sent_at IS NOT NULL AND
                                                  r.concluded_at IS NULL )        AS count_of_live_referrals,
                       COUNT(r.id) FILTER ( WHERE r.concluded_at IS NOT NULL AND
                                                  r.end_requested_at IS NOT NULL AND
                                                  eosr.submitted_at IS NULL )     AS count_of_cancelled_referrals,
                       COUNT(r.id) FILTER ( WHERE r.concluded_at IS NOT NULL AND
                                                  r.end_requested_at IS NOT NULL AND
                                                  eosr.submitted_at IS NOT NULL ) AS count_of_early_end_referrals,
                       COUNT(r.id) FILTER ( WHERE r.concluded_at IS NOT NULL AND
                                                  r.end_requested_at IS NULL )    AS count_of_completed_referrals,
                       COUNT(r.id) FILTER ( WHERE r.concluded_at IS NOT NULL )    AS count_of_concluded_referrals
                FROM referral r
                         LEFT JOIN end_of_service_report eosr ON r.id = eosr.referral_id,
                     dates d
                WHERE COALESCE(r.concluded_at, r.end_requested_at, r.sent_at, r.created_at) BETWEEN d.start_date AND d.end_date),

     times AS (SELECT PERCENTILE_CONT(0.50) WITHIN GROUP ( ORDER BY sent_at - created_at ) AS p50_completion_time,
                      PERCENTILE_CONT(0.95) WITHIN GROUP ( ORDER BY sent_at - created_at ) AS p95_completion_time,
                      PERCENTILE_CONT(0.99) WITHIN GROUP ( ORDER BY sent_at - created_at ) AS p99_completion_time
               FROM referral r,
                    dates d
               WHERE sent_at IS NOT NULL
                 AND COALESCE(r.concluded_at, r.end_requested_at, r.sent_at, r.created_at) BETWEEN d.start_date AND d.end_date)

SELECT dates.start_date::date   AS start_date_inclusive,
       dates.end_date::date - 1 AS end_date_inclusive,
       counts.count_of_started_referrals,
       counts.count_of_draft_referrals,
       counts.count_of_sent_referrals,
       counts.count_of_live_referrals,
       counts.count_of_cancelled_referrals,
       counts.count_of_early_end_referrals,
       counts.count_of_completed_referrals,
       counts.count_of_concluded_referrals,
       times.p50_completion_time,
       times.p95_completion_time,
       times.p99_completion_time

FROM dates,
     counts,
     times;
