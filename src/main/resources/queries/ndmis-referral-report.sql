WITH cte
         AS (SELECT referral_id,
                    assigned_to_id,
                    Rank ()
                        OVER (
           partition BY referral_id
           ORDER BY assigned_at DESC ) current_assignment_rank
             FROM   referral_assignments),
     action_plan_first_submitted_at
         AS (SELECT a.submitted_at,
                    a.referral_id,
                    Rank ()
                        OVER (
           partition BY a.referral_id
           ORDER BY a.submitted_at ASC ) action_plan_submitted_rank
             FROM   action_plan a
                        JOIN referral r
                             ON a.referral_id = r.id
             WHERE  a.submitted_at IS NOT NULL),
     action_plan_first_approved_at
         AS (SELECT a.approved_at,
                    a.referral_id,
                    a.number_of_sessions,
                    Rank ()
                        OVER (
           partition BY a.referral_id
           ORDER BY a.approved_at ASC ) action_plan_approved_rank
             FROM   action_plan a
                        JOIN referral r
                             ON a.referral_id = r.id
             WHERE  a.approved_at IS NOT NULL),
     action_plan_finally_approved_at
         AS (SELECT a.approved_at,
                    a.referral_id,
                    a.number_of_sessions,
                    Rank ()
                        OVER (
             partition BY a.referral_id
             ORDER BY a.approved_at desc ) action_plan_finally_approved_rank
             FROM   action_plan a
                        JOIN referral r
                             ON a.referral_id = r.id
             WHERE a.approved_at IS NOT null),
     action_plan_latest_approved_at
         AS (SELECT a.id,
                    a.approved_at,
                    a.referral_id,
                    a.number_of_sessions,
                    Rank ()
                        OVER (
           partition BY a.referral_id
           ORDER BY a.approved_at DESC )
           action_plan_latest_approved_rank
             FROM   action_plan a
                        JOIN referral r
                             ON a.referral_id = r.id
             WHERE  a.approved_at IS NOT NULL),
     desired_outcomes_size
         AS (SELECT DISTINCT referral_id,
                             Count(1)
                                 OVER (
  partition BY referral_id) AS numberOfOutcomes
             FROM   referral_desired_outcome),
     dsa_completed
         AS (SELECT count(*) AS numberOfSessionsAttended,
                    a.referral_id
             FROM   appointment a
                        JOIN delivery_session_appointment dsa
                             ON a.id = dsa.appointment_id
                        JOIN referral r
                             ON r.id = a.referral_id
             WHERE a.stale = false
               AND a.superseded = false
               AND (
                 (a.did_session_happen is null AND (a.attended = 'YES' OR a.attended = 'LATE' )) OR
                 (a.did_session_happen = true AND a.attended = 'YES'))
             Group BY a.referral_id
     ),

     achievement_score_cte AS (
         SELECT
             esr.referral_id,
             CASE
                 WHEN esr.submitted_at IS NOT NULL THEN
                     SUM(
                             CASE eos.achievement_level
                                 WHEN 'ACHIEVED' THEN 1.0
                                 WHEN 'PARTIALLY_ACHIEVED' THEN 0.5
                                 ELSE 0.0
                                 END
                     )
                 ELSE 0.0
                 END AS outcomes_to_be_achieved_count
         FROM end_of_service_report esr
                  JOIN end_of_service_report_outcome eos ON eos.end_of_service_report_id = esr.id
         GROUP BY esr.referral_id, esr.submitted_at
     )

SELECT r.reference_number                               AS referral_ref,
       r.id                                             AS referral_id,
       d.contract_reference                             AS crs_contract_reference,
       ct.name                                          AS crs_contract_type,
       d.prime_provider_id                              AS crs_provider_id,
       cau.user_name                                    AS referring_officer_id,
       r.relevant_sentence_id                           AS relevant_sentence_id,
       r.service_usercrn                                AS service_user_crn,
       r.sent_at                                        AS date_referral_received,
       action_plan_first_submitted_at.submitted_at      AS date_first_action_plan_submitted,
       action_plan_first_approved_at.approved_at        AS date_of_first_action_plan_approval,
       rd.numberofoutcomes                              AS outcomes_to_be_achieved_count,
       COALESCE(eosras.outcomes_to_be_achieved_count,0.0) AS outcomes_progress,
       COALESCE(action_plan_latest_approved_at.number_of_sessions,0) AS count_of_sessions_expected,
       COALESCE(dsa_completed.numberOfSessionsAttended,0) AS count_of_sessions_attended,
       r.end_requested_at                               AS date_intervention_ended,
       CASE
           WHEN r.concluded_at IS NULL THEN NULL
           WHEN r.end_requested_at IS NULL THEN 'completed'
           WHEN es.id IS NULL THEN 'cancelled'
           ELSE 'ended'
           END AS intervention_end_reason,

       es.submitted_at                                  AS date_end_of_service_report_submitted,
       coalesce(r.withdrawal_reason_code, r.end_requested_reason_code)         AS intervention_end_reason_code,
       coalesce(wr.description, cr.description)         AS intervention_end_reason_description,

       r.concluded_at                                   AS intervention_concluded_at
FROM   referral r
           LEFT OUTER JOIN intervention i
                           ON r.intervention_id = i.id
           LEFT OUTER JOIN dynamic_framework_contract d
                           ON d.id = i.dynamic_framework_contract_id
           LEFT OUTER JOIN contract_type ct
                           ON ct.id = d.contract_type_id
           LEFT OUTER JOIN cte rass
                           ON rass.referral_id = r.id
                               AND rass.current_assignment_rank = 1
           LEFT OUTER JOIN action_plan_first_submitted_at
                           ON action_plan_first_submitted_at.referral_id = r.id
                               AND
                              action_plan_first_submitted_at.action_plan_submitted_rank = 1
           LEFT OUTER JOIN action_plan_first_approved_at
                           ON action_plan_first_approved_at.referral_id = r.id
                               AND
                              action_plan_first_approved_at.action_plan_approved_rank = 1
           LEFT OUTER JOIN action_plan_latest_approved_at
                           ON action_plan_latest_approved_at.referral_id = r.id
                               AND
                              action_plan_latest_approved_at.action_plan_latest_approved_rank = 1
           LEFT OUTER JOIN dsa_completed
                           ON dsa_completed.referral_id = r.id
           LEFT OUTER JOIN auth_user au
                           ON au.id = rass.assigned_to_id
           LEFT OUTER JOIN auth_user cau
                           ON cau.id = r.created_by_id
           LEFT OUTER JOIN desired_outcomes_size rd
                           ON rd.referral_id = r.id
           LEFT OUTER JOIN end_of_service_report es
                           ON es.referral_id = r.id
           LEFT OUTER JOIN cancellation_reason cr
                           ON cr.code = r.end_requested_reason_code
           LEFT OUTER JOIN withdrawal_reason wr
                           ON wr.code = r.withdrawal_reason_code
           LEFT OUTER JOIN referral_details rde
                           ON rde.referral_id = r.id
           LEFT OUTER JOIN achievement_score_cte eosras ON eosras.referral_id = r.id
ORDER BY r.sent_at ASC

