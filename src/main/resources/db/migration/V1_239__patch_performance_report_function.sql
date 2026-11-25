CREATE OR REPLACE FUNCTION performance_report(
    contract_references text[],
    sent_from timestamp,
    sent_to timestamp
)
RETURNS TABLE (
    referral_id UUID,
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
    completion_deadline date,
    end_of_sentence_date date
)
AS $body$
WITH selected_referrals AS MATERIALIZED (
    SELECT r.*
    FROM referral r
    JOIN intervention i ON i.id = r.intervention_id
    JOIN dynamic_framework_contract d ON d.id = i.dynamic_framework_contract_id
    WHERE d.contract_reference = ANY ($1)
      AND r.sent_at > $2
      AND r.sent_at < $3
),
cte AS (
    SELECT ra.referral_id,
           ra.assigned_to_id,
           RANK() OVER (
               PARTITION BY ra.referral_id
               ORDER BY ra.assigned_at DESC
           ) AS current_assignment_rank
    FROM referral_assignments ra
    JOIN selected_referrals r ON r.id = ra.referral_id
),
saa AS (
    SELECT a.referral_id,
           a.created_at,
           a.appointment_time AS appointmenttime,
           RANK() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.created_at ASC
           ) AS supplier_assessment_appointment_rank
    FROM appointment a
    JOIN supplier_assessment_appointment sa ON sa.appointment_id = a.id
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.stale = false
),
saa_primary AS (
    SELECT a.referral_id,
           a.created_at,
           a.appointment_time AS appointmenttime,
           RANK() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.created_at ASC
           ) AS supplier_assessment_appointment_rank
    FROM appointment a
    JOIN supplier_assessment_appointment sa ON sa.appointment_id = a.id
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.stale = false
      AND a.superseded = false
),
saa_not_attended AS (
    SELECT a.referral_id,
           a.appointment_time AS notattendedappointmenttime,
           ROW_NUMBER() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.appointment_time ASC
           ) AS saa_not_attended_rank
    FROM appointment a
    JOIN supplier_assessment_appointment sa ON sa.appointment_id = a.id
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.stale = false
      AND a.attended = 'NO'
),
saa_attended_late AS (
    SELECT a.referral_id,
           a.appointment_time AS attendedorlateappointmenttime,
           a.attended,
           ROW_NUMBER() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.appointment_time ASC
           ) AS saa_attended_late_rank
    FROM appointment a
    JOIN supplier_assessment_appointment sa ON sa.appointment_id = a.id
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.stale = false
      AND a.appointment_feedback_submitted_at IS NOT NULL
      AND (a.attended = 'YES' OR a.attended = 'LATE')
),
saa_completed AS (
    SELECT a.referral_id,
           a.appointment_time AS attendedorlateappointmenttime,
           a.attended,
           ROW_NUMBER() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.appointment_time ASC
           ) AS saa_completed_rank
    FROM appointment a
    JOIN supplier_assessment_appointment sa ON sa.appointment_id = a.id
    JOIN selected_referrals r ON r.id = a.referral_id
    -- Preserve the original OR branch, which does not check stale/superseded.
    WHERE (
        a.stale = false
        AND a.superseded = false
        AND a.did_session_happen IS NULL
        AND (a.attended = 'YES' OR a.attended = 'LATE')
    )
    OR (
        a.did_session_happen = true
        AND a.attended = 'YES'
    )
),
action_plan_first_submitted_at AS (
    SELECT a.submitted_at,
           a.referral_id,
           RANK() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.submitted_at ASC
           ) AS action_plan_submitted_rank
    FROM action_plan a
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.submitted_at IS NOT NULL
),
action_plan_first_approved_at AS (
    SELECT a.approved_at,
           a.referral_id,
           a.number_of_sessions,
           RANK() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.approved_at ASC
           ) AS action_plan_approved_rank
    FROM action_plan a
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.approved_at IS NOT NULL
),
action_plan_finally_approved_at AS (
    SELECT a.approved_at,
           a.referral_id,
           a.number_of_sessions,
           RANK() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.approved_at DESC
           ) AS action_plan_finally_approved_rank
    FROM action_plan a
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.approved_at IS NOT NULL
),
action_plan_latest_approved_at AS (
    SELECT a.id,
           a.approved_at,
           a.referral_id,
           a.number_of_sessions,
           RANK() OVER (
               PARTITION BY a.referral_id
               ORDER BY a.approved_at DESC
           ) AS action_plan_latest_approved_rank
    FROM action_plan a
    JOIN selected_referrals r ON r.id = a.referral_id
    WHERE a.approved_at IS NOT NULL
),
desired_outcomes_size AS (
    SELECT DISTINCT
           o.referral_id,
           COUNT(1) OVER (
               PARTITION BY o.referral_id
           ) AS numberofoutcomes
    FROM referral_desired_outcome o
    JOIN selected_referrals r ON r.id = o.referral_id
)
SELECT r.id AS referral_id,
       r.reference_number AS referral_reference,
       d.contract_reference AS contract_reference,
       d.prime_provider_id AS organisation_id,
       au.user_name AS current_assignee_email,
       r.service_usercrn AS crn,
       r.sent_at AS date_referral_received,
       saa.created_at AS date_supplier_assessment_first_arranged,
       saa_primary.appointmenttime AS date_supplier_assessment_first_scheduled_for,
       saa_not_attended.notattendedappointmenttime
           AS date_supplier_assessment_first_not_attended,
       saa_attended_late.attendedorlateappointmenttime
           AS date_supplier_assessment_first_attended,
       saa_completed.attendedorlateappointmenttime
           AS date_supplier_assessment_first_completed,
       CAST(saa_attended_late.attended = 'YES' AS text)
           AS supplier_assessment_attended_on_time,
       action_plan_first_submitted_at.submitted_at AS first_action_plan_submitted_at,
       action_plan_first_approved_at.approved_at AS first_action_plan_approved_at,
       action_plan_latest_approved_at.id AS approved_action_plan_id,
       CAST(rd.numberofoutcomes AS integer) AS number_of_outcomes,
       es.id AS end_of_service_report_id,
       action_plan_latest_approved_at.number_of_sessions AS number_of_sessions,
       r.end_requested_at,
       COALESCE(wr.description, cr.description) AS end_requested_reason,
       es.submitted_at AS eosr_submitted_at,
       r.concluded_at,
       rde.completion_deadline,
       r.relevant_sentence_end_date AS end_of_sentence_date
FROM selected_referrals r
LEFT JOIN intervention i ON r.intervention_id = i.id
LEFT JOIN dynamic_framework_contract d ON d.id = i.dynamic_framework_contract_id
LEFT JOIN cte rass
       ON rass.referral_id = r.id
      AND rass.current_assignment_rank = 1
LEFT JOIN saa
       ON saa.referral_id = r.id
      AND saa.supplier_assessment_appointment_rank = 1
LEFT JOIN saa_primary
       ON saa_primary.referral_id = r.id
      AND saa_primary.supplier_assessment_appointment_rank = 1
LEFT JOIN saa_not_attended
       ON saa_not_attended.referral_id = r.id
      AND saa_not_attended.saa_not_attended_rank = 1
LEFT JOIN saa_attended_late
       ON saa_attended_late.referral_id = r.id
      AND saa_attended_late.saa_attended_late_rank = 1
LEFT JOIN saa_completed
       ON saa_completed.referral_id = r.id
      AND saa_completed.saa_completed_rank = 1
LEFT JOIN action_plan_first_submitted_at
       ON action_plan_first_submitted_at.referral_id = r.id
      AND action_plan_first_submitted_at.action_plan_submitted_rank = 1
LEFT JOIN action_plan_first_approved_at
       ON action_plan_first_approved_at.referral_id = r.id
      AND action_plan_first_approved_at.action_plan_approved_rank = 1
LEFT JOIN action_plan_latest_approved_at
       ON action_plan_latest_approved_at.referral_id = r.id
      AND action_plan_latest_approved_at.action_plan_latest_approved_rank = 1
LEFT JOIN auth_user au ON au.id = rass.assigned_to_id
LEFT JOIN desired_outcomes_size rd ON rd.referral_id = r.id
LEFT JOIN end_of_service_report es ON es.referral_id = r.id
LEFT JOIN cancellation_reason cr ON cr.code = r.end_requested_reason_code
LEFT JOIN withdrawal_reason wr ON wr.code = r.withdrawal_reason_code
LEFT JOIN referral_details rde ON rde.referral_id = r.id
WHERE rde.superseded_by_id IS NULL
ORDER BY r.sent_at ASC;
$body$
LANGUAGE sql;