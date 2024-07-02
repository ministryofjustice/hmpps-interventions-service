create or replace function performance_report(contract_references text[], sent_from timestamp, sent_to timestamp)
  returns table (referral_id UUID,
                          referral_reference text,
                          contract_reference text,
                          organisation_id text,
                          current_assignee_email text,
                          crn text,
                          date_referral_received timestamp,
                          date_supplier_assessment_first_arranged timestamp,
                          date_supplier_assessment_first_scheduled_for timestamp,
                          date_supplier_assessment_first_not_attended timestamp,
                          date_supplier_assessment_first_attended timestamp,
                          date_supplier_assessment_first_completed timestamp,
                          supplier_assessment_attended_on_time text,
                          first_action_plan_submitted_at timestamp,
                          first_action_plan_approved_at timestamp,
                          approved_action_plan_id UUID,
                          number_of_outcomes integer,
                          end_of_service_report_id UUID,
                          number_of_sessions integer,
                          end_requested_at timestamp,
                          end_requested_reason text,
                          eosr_submitted_at timestamp,
                          concluded_at timestamp,
                          completion_deadline date)
as
$body$
WITH cte
AS (SELECT referral_id,
           assigned_to_id,
           Rank ()
           OVER (
           partition BY referral_id
           ORDER BY assigned_at DESC ) current_assignment_rank
  FROM   referral_assignments),
  saa
AS (SELECT a.referral_id,
           a.created_at,
           a.appointment_time               AS appointmentTime,
           Rank ()
           OVER (
           partition BY a.referral_id
           ORDER BY a.created_at ASC )
           supplier_assessment_appointment_rank
  FROM   appointment a
  JOIN supplier_assessment_appointment saa
  ON a.id = saa.appointment_id
  JOIN referral r
  ON r.id = a.referral_id
  WHERE a.stale = false),
  saa_not_attended
AS (SELECT a.referral_id,
           a.appointment_time                    AS
           notAttendedAppointmentTime,
           ROW_NUMBER ()
           OVER (
           partition BY a.referral_id
           ORDER BY a.appointment_time ASC ) saa_not_attended_rank
  FROM   appointment a
  JOIN supplier_assessment_appointment saa
  ON a.id = saa.appointment_id
  JOIN referral r
  ON r.id = a.referral_id
  WHERE a.stale = false AND a.attended = 'NO'),
  saa_attended_late
AS (SELECT a.referral_id,
           a.appointment_time                    AS
           attendedOrLateAppointmentTime,
           a.attended,
           ROW_NUMBER ()
           OVER (
           partition BY a.referral_id
           ORDER BY a.appointment_time ASC ) saa_attended_late_rank
  FROM   appointment a
  JOIN supplier_assessment_appointment saa
  ON a.id = saa.appointment_id
  JOIN referral r
  ON r.id = a.referral_id
  WHERE a.stale = false
  AND ( a.attended = 'YES'
  OR a.attended = 'LATE' )),
  saa_completed
  AS (SELECT a.referral_id,
             a.appointment_time                    AS
             attendedOrLateAppointmentTime,
             a.attended,
             ROW_NUMBER ()
             OVER (
             partition BY a.referral_id
             ORDER BY a.appointment_time ASC ) saa_completed_rank
    FROM   appointment a
    JOIN supplier_assessment_appointment saa
    ON a.id = saa.appointment_id
    JOIN referral r
    ON r.id = a.referral_id
    WHERE a.stale = false
    AND (
      (a.did_session_happen = null AND (a.attended = 'YES' OR a.attended = 'LATE' ))) OR
      (a.did_session_happen = true AND (a.attended = 'YES'))
    ),
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
  FROM   referral_desired_outcome)
  SELECT r.id                                             AS referral_id,
         r.reference_number                               AS referral_reference,
         d.contract_reference                             AS contract_reference,
         d.prime_provider_id                              AS organisation_id,
         au.user_name                                     AS current_assignee_email,
         r.service_usercrn                                AS crn,
         r.sent_at                                        AS date_referral_received,
         saa.created_at                                   AS date_supplier_assessment_first_arranged,
         saa.appointmenttime                              AS date_supplier_assessment_first_scheduled_for,
         saa_not_attended.notattendedappointmenttime      AS date_supplier_assessment_first_not_attended,
         saa_attended_late.attendedorlateappointmenttime  AS date_supplier_assessment_first_attended,
         saa_completed.attendedorlateappointmenttime      AS date_supplier_assessment_first_completed,
         ( saa_attended_late.attended = 'YES' )           AS supplier_assessment_attended_on_time,
         action_plan_first_submitted_at.submitted_at      AS first_action_plan_submitted_at,
         action_plan_first_approved_at.approved_at        AS first_action_plan_approved_at,
         action_plan_latest_approved_at.id                AS approved_action_plan_id,
         rd.numberofoutcomes                              AS number_of_outcomes,
         es.id                                            AS end_of_service_report_id,
         action_plan_latest_approved_at.number_of_sessions AS number_of_sessions,
         r.end_requested_at                               AS end_requested_at,
         cr.description                                   AS end_requested_reason,
         es.submitted_at                                  AS eosr_submitted_at,
         r.concluded_at                                   AS concluded_at,
         rde.completion_deadline                          AS completion_deadline
  FROM   referral r
  LEFT OUTER JOIN intervention i
  ON r.intervention_id = i.id
  LEFT OUTER JOIN dynamic_framework_contract d
  ON d.id = i.dynamic_framework_contract_id
  LEFT OUTER JOIN cte rass
  ON rass.referral_id = r.id
  AND rass.current_assignment_rank = 1
  LEFT OUTER JOIN saa
  ON saa.referral_id = r.id
  AND saa.supplier_assessment_appointment_rank = 1
  LEFT OUTER JOIN saa_not_attended
  ON saa_not_attended.referral_id = r.id
  AND saa_not_attended.saa_not_attended_rank = 1
  LEFT OUTER JOIN saa_attended_late
  ON saa_attended_late.referral_id = r.id
  AND saa_attended_late.saa_attended_late_rank = 1
  LEFT OUTER join saa_completed
  ON saa_completed.referral_id = r.id
  AND saa_completed.saa_completed_rank = 1
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
  LEFT OUTER JOIN auth_user au
  ON au.id = rass.assigned_to_id
  LEFT OUTER JOIN desired_outcomes_size rd
  ON rd.referral_id = r.id
  LEFT OUTER JOIN end_of_service_report es
  ON es.referral_id = r.id
  LEFT OUTER JOIN cancellation_reason cr
  ON cr.code = r.end_requested_reason_code
  LEFT OUTER JOIN referral_details rde
  ON rde.referral_id = r.id
  WHERE d.contract_reference = ANY ($1)
  AND r.sent_at > ($2)
  AND r.sent_at < ($3)
  ORDER BY r.sent_at ASC;
$body$
language sql;