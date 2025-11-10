WITH delivery_appointments AS (
    SELECT
        r.reference_number as referral_ref,
        r.id AS referral_id,
        a.id as appointmentId,
        a.appointment_time as appointment_time ,
        a.duration_in_minutes as duration_in_minutes,
        a.created_at as booked_at,
        a.attended as attended,
        a.attendance_submitted_at as attendance_submitted_at,
        a.notify_probation_practitioner_of_behaviour as notifyppof_attendance_behaviour,
        a.delius_appointment_id as delius_appointment_id,
        'DELIVERY' AS reason_for_appointment
    FROM appointment a
             INNER JOIN delivery_session_appointment ds ON a.id = ds.appointment_id
             INNER JOIN referral r ON r.id = a.referral_id
    where a.referral_id= :referralId
),
     supplier_appointments AS (
         SELECT
             r.reference_number as referral_ref,
             r.id AS referral_id,
             a.id as appointmentId,
             a.appointment_time as appointment_time ,
             a.duration_in_minutes as duration_in_minutes,
             a.created_at as booked_at,
             a.attended as attended,
             a.attendance_submitted_at as attendance_submitted_at,
             a.notify_probation_practitioner_of_behaviour as notifyppof_attendance_behaviour,
             a.delius_appointment_id as delius_appointment_id,
             'SAA' AS reason_for_appointment
         FROM appointment a
                  INNER JOIN supplier_assessment_appointment sa ON a.id = sa.appointment_id
                  INNER JOIN referral r ON r.id = a.referral_id
         where a.referral_id= :referralId
     )
SELECT *
FROM (
         SELECT * FROM delivery_appointments
         UNION ALL
         SELECT * FROM supplier_appointments
     ) combined_appointments
ORDER BY appointment_time, referral_id;