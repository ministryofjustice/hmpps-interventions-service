create or replace function performance_report_first_attendance(referral_id UUID)
  returns timestamp
as
$body$
SELECT MIN(a.appointment_time)
FROM appointment a
WHERE a.referral_id = $1
	AND a.stale = false
	AND a.appointment_feedback_submitted_at is not null
	AND ((a.did_session_happen is null AND (a.attended = 'LATE' OR a.attended = 'YES'))
	OR (a.did_session_happen = true AND a.attended = 'YES'))
    AND NOT EXISTS
    (
        SELECT 1
        FROM supplier_assessment_appointment saa
        WHERE saa.appointment_id = a.id
    )
$body$
language sql;