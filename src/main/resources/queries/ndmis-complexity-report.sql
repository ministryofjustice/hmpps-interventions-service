SELECT
    r.reference_number AS referral_ref,
    r.id AS referral_id,
    i.title AS intervention_title,
    sc.id AS service_category_id,
    sc.name AS service_category_name,
    cl.title AS complexity_level_title
FROM referral r
         INNER JOIN intervention i ON r.intervention_id = i.id
         INNER JOIN referral_selected_service_category rssc ON r.id = rssc.referral_id
         INNER JOIN service_category sc ON rssc.service_category_id = sc.id
         LEFT JOIN referral_complexity_level_ids rcli ON r.id = rcli.referral_id
    AND rcli.complexity_level_ids_key = sc.id
         LEFT JOIN complexity_level cl ON sc.id = cl.service_category_id
    AND cl.id = rcli.complexity_level_ids
ORDER BY r.id, sc.id;