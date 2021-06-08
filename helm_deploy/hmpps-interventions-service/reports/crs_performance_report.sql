COPY (
  WITH attended_sessions AS (
      select count(action_plan_id) AS attended, action_plan_id
      from action_plan_appointment
      where attended in ('YES', 'LATE')
      group by action_plan_id
  ),
  attempted_sessions AS (
      select count(action_plan_id) AS attempted, action_plan_id
      from action_plan_appointment
      where attended IS NOT NULL
      group by action_plan_id
  )
  SELECT
    r.reference_number      AS referral_ref,
    r.id                    AS referral_id,
    c.contract_reference    AS crs_contract_reference,
    ct.code                 AS crs_contract_type,
    prime.id                AS crs_provider_id,
    r.sent_by_id            AS referring_officer_id,
    r.relevant_sentence_id  AS relevant_sentence_id,
    r.service_usercrn       AS service_user_crn,
    r.sent_at               AS date_referral_received,
    'coming-later'          AS date_saa_booked,
    'coming-later'          AS date_saa_attended,
    ap.submitted_at         AS date_first_action_plan_submitted,
    'coming-later'          AS date_of_first_action_plan_approval,
    (
        select min(app.appointment_time)
        from session_delivery_appointment ses
        join appointment app on ses.appointment_id = app.id
        where ses.action_plan_id = ap.id and app.attended in ('YES', 'LATE')
    )                       AS date_of_first_session,
    (
      select count(o.desired_outcome_id)
      from referral_desired_outcome o
      where o.referral_id = r.id
    )                       AS outcomes_to_be_achieved_count,
    'coming-later'          AS outcomes_progress,
    ap.number_of_sessions   AS count_of_sessions_expected,
    shows.attended          AS count_of_sessions_attended,
    r.concluded_at          AS date_intervention_ended,
    (
      CASE
        WHEN r.concluded_at IS NOT NULL AND eosr.id IS NULL THEN 'cancelled'
        WHEN r.concluded_at IS NOT NULL AND eosr.id IS NOT NULL AND ap.number_of_sessions > atts.attempted THEN 'ended'
        WHEN r.concluded_at IS NOT NULL AND eosr.id IS NOT NULL AND ap.number_of_sessions = atts.attempted THEN 'completed'
      END
    )                       AS intervention_end_reason,
    eosr.submitted_at       AS date_end_of_service_report_submitted
  FROM
    referral r
    JOIN intervention i ON (r.intervention_id = i.id)
    JOIN dynamic_framework_contract c ON (i.dynamic_framework_contract_id = c.id)
    JOIN contract_type ct ON (c.contract_type_id = ct.id)
    JOIN service_provider prime ON (c.prime_provider_id = prime.id)
    LEFT JOIN action_plan ap ON (ap.referral_id = r.id) --❗️assumes a SINGLE action plan
    LEFT JOIN attended_sessions shows ON (shows.action_plan_id = ap.id) --❗️should be linked to referrals instead, sessions are static
    LEFT JOIN attempted_sessions atts ON (atts.action_plan_id = ap.id) --❗️should be linked to referrals instead, sessions are static
    LEFT JOIN end_of_service_report eosr ON (eosr.referral_id = r.id)
  WHERE
    r.sent_at IS NOT NULL
  ORDER BY
    r.sent_at
) TO STDOUT WITH CSV HEADER
