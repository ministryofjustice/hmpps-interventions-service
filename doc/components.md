# Components

## Jobs

We use Spring Batch to create one-off and scheduled jobs.

For a one-off job:

1. Create a new package under `uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff`
2. For an example job, see [#1286](https://github.com/ministryofjustice/hmpps-interventions-service/pull/1286)

For a scheduled job:

1. Create a new package under `uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.scheduled`
2. For an example job, see [the NDMIS report job](https://github.com/ministryofjustice/hmpps-interventions-service/tree/5c1ef9aee2176af88993a68cd2514738132bcba9/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsinterventionsservice/reporting/ndmis/performance)

For both:

3. Provide an `ApplicationRunner` `@Bean` to make the job callable from command line, see [#1348](https://github.com/ministryofjustice/hmpps-interventions-service/pull/1348) or [this example](https://github.com/ministryofjustice/hmpps-interventions-service/blob/5c1ef9aee2176af88993a68cd2514738132bcba9/src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsinterventionsservice/reporting/ndmis/performance/NdmisPerformanceReportJobConfiguration.kt#L59-L60)
4. Create a `CronJob` or a `Job` in `helm_deploy` that will let it be executed on the environments, see [this example](https://github.com/ministryofjustice/hmpps-interventions-service/blob/5c1ef9aee2176af88993a68cd2514738132bcba9/helm_deploy/hmpps-interventions-service/templates/cronjob-migrate-referral-details-to-changelog-report.yaml)

## Running

For both kind of jobs, running locally:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --args='--jobName=changelogMigrationJob'
```

To run on environments, assuming a `CronJob`, please see [triggering scheduled jobs](https://ministryofjustice.github.io/hmpps-interventions-docs/runbooks/trigger-scheduled-jobs.html) from the runbook.
