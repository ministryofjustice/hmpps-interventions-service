-- found by going to the https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/meta/schema/ page and filtering for "deprecated"
DROP VIEW delivery_session_appointment_deprecated;
DROP VIEW delivery_session_deprecated;
DROP TABLE deprecated_action_plan_appointment;
DROP TABLE deprecated_action_plan_session_appointment;
DROP TABLE deprecated_action_plan_session;

DELETE
FROM metadata
WHERE table_name IN ('deprecated_action_plan_appointment',
                     'deprecated_action_plan_session_appointment',
                     'deprecated_action_plan_session');
