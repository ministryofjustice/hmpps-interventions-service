CREATE VIEW referral_summary AS
WITH cte
AS (SELECT referral_id,
           assigned_to_id,
           Rank ()
           OVER (
           partition BY referral_id
           ORDER BY assigned_at DESC ) current_assignment_rank
  FROM   referral_assignments),
  supplier_appointment
AS (SELECT referral_id,
           id,
           Rank ()
           OVER (
           partition BY referral_id
           ORDER BY appointment_time DESC ) latest_appointment_rank
  FROM  appointment a join supplier_assessment_appointment sa on a.id= sa.appointment_id)
  SELECT r.id                           AS id,
         r.sent_at                      AS sent_at,
         r.sent_by_id                   AS sent_by,
         r.reference_number             AS reference_number,
         r.service_usercrn              AS crn,
         r.created_by_id                AS created_by,
         au.id                          AS assigned_to_id,
         au.auth_source                 AS assigned_auth_source,
         au.user_name                   AS assigned_user_name,
         sau.auth_source                AS sent_auth_source,
         sau.user_name                  AS sent_user_name,
         rs.first_name                  AS service_user_first_name,
         rs.last_name                   AS service_user_last_name,
         sp.id                          AS service_provider_id,
         sp.name                        AS service_provider_name,
         i.title                        AS intervention_title,
         r.concluded_at                 AS concluded_at,
         rl.expected_release_date       AS expected_release_date,
         rl.referral_releasing_12_weeks AS is_referral_releasing_in12weeks,
         rl.type                        AS referral_type,
         rl.prison_id                   AS prison_id,
         ppd.probation_office           AS probation_office,
         ppd.pdu                        AS pdu,
         ppd.ndelius_pdu                AS ndelius_pdu,
         d.id                           AS contract_id,
         r.end_requested_at             AS end_requested_at,
         es.id                          AS eosr_id,
         saa.id                         AS appointment_id
  FROM   referral r
  LEFT OUTER JOIN cte rass
  ON rass.referral_id = r.id
  AND rass.current_assignment_rank = 1
  LEFT OUTER JOIN supplier_appointment saa
  ON saa.referral_id = r.id
  AND saa.latest_appointment_rank = 1
  LEFT OUTER JOIN referral_location rl
  ON rl.referral_id = r.id
  LEFT OUTER JOIN auth_user au
  ON rass.assigned_to_id = au.id
  LEFT OUTER JOIN auth_user sau
  ON r.sent_by_id = sau.id
  LEFT OUTER JOIN referral_service_user_data rs
  ON rs.referral_id = r.id
  LEFT OUTER JOIN intervention i
  ON r.intervention_id = i.id
  LEFT OUTER JOIN dynamic_framework_contract d
  ON d.id = i.dynamic_framework_contract_id
  LEFT OUTER JOIN service_provider sp
  ON sp.id = d.prime_provider_id
  LEFT OUTER JOIN probation_practitioner_details ppd
  ON ppd.referral_id = r.id
  LEFT OUTER JOIN end_of_service_report es
  ON es.referral_id = r.id;

INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','id', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','sent_at', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','sent_by', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','reference_number', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','crn', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','created_by', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','assigned_to_id', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','assigned_auth_source', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','assigned_user_name', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','sent_auth_source', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','sent_user_name', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','service_user_first_name', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','service_user_last_name', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','service_provider_id', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','service_provider_name', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','intervention_title', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','concluded_at', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','expected_release_date', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','is_referral_releasing_in12weeks', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','referral_type', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','prison_id', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','probation_office', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','pdu', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','ndelius_pdu', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','contract_id', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','end_requested_at', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','eosr_id', TRUE, TRUE);
INSERT INTO metadata (table_name, column_name, sensitive, domain_data) VALUES ('referral_summary','appointment_id', TRUE, TRUE);