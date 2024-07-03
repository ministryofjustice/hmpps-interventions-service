create or replace function performance_report_attendances_count(referral_id UUID)
  returns integer
as
$body$
SELECT count(*)
FROM appointment a
WHERE a.referral_id = $1
	AND a.stale = false
	AND (a.superseded is null OR a.superseded = false)
	AND ((a.did_session_happen is null AND (a.late = true OR a.attended = 'YES'))
	OR (a.did_session_happen = true AND a.attended = 'YES'))
	AND NOT EXISTS
    (
        SELECT 1
        FROM supplier_assessment_appointment saa
        WHERE saa.appointment_id = a.id
    )
$body$
language sql;