create or replace function performance_report_first_attendance(referral_id UUID)
  returns timestamp
as
$body$
SELECT MIN(a.appointment_time)
FROM appointment a
WHERE a.referral_id = $1
	AND a.stale = false
	AND ((a.did_session_happen is null AND (a.late = true OR a.attended = 'YES'))
	OR (a.did_session_happen = true AND a.attended = 'YES'))
$body$
language sql;