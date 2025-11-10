SELECT
    r.reference_number as referral_ref,
    r.id AS referral_id,
    a.id as appointmentId,
    a.appointment_time as appointment_time,
    a.duration_in_minutes as duration_in_minutes,
    a.created_at as booked_at,
    a.attended as attended,
    a.attendance_submitted_at as attendance_submitted_at,
    a.notify_probation_practitioner_of_behaviour as notifyppof_attendance_behaviour,
    a.delius_appointment_id as delius_appointment_id,
    CASE
        WHEN ds.appointment_id IS NOT NULL THEN 'DELIVERY'
        WHEN sa.appointment_id IS NOT NULL THEN 'SAA'
        END AS reason_for_appointment
FROM appointment a
         INNER JOIN referral r ON r.id = a.referral_id
         LEFT JOIN delivery_session_appointment ds ON a.id = ds.appointment_id
         LEFT JOIN supplier_assessment_appointment sa ON a.id = sa.appointment_id
WHERE ds.appointment_id IS NOT NULL OR sa.appointment_id IS NOT NULL
ORDER BY r.id, a.appointment_time;