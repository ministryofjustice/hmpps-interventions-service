-- found by going to the https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/meta/schema/columns.html page and filtering for "deprecated"
ALTER TABLE referral
    DROP COLUMN deprecated_assigned_at,
    DROP COLUMN deprecated_assigned_by_id,
    DROP COLUMN deprecated_assigned_to_id;

DELETE
FROM metadata
WHERE column_name IN ('deprecated_assigned_at',
                      'deprecated_assigned_by_id',
                      'deprecated_assigned_to_id');
