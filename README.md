# HMPPS Interventions Service

[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/swagger-ui.html)

Business/domain API to **find, arrange and monitor an intervention** for service users (offenders).

## Quickstart

Please see the [quickstart document](doc/quickstart.md).

For documentation about configuring one-off or scheduled jobs, see [the components document](doc/components.md).

Common tasks and runbooks are
in [the service's technical guidance](https://ministryofjustice.github.io/hmpps-interventions-docs/runbooks/).

## Architecture

To see where this service fits in the broader interventions (and probation) architecture, you can browse the HMPPS C4
models [here](https://structurizr.com/share/56937/diagrams#interventions-container).

- [Decisions specific to this application](doc/adr)
- [Decisions relevant to the "interventions" domain](https://github.com/ministryofjustice/hmpps-interventions-docs)

## Code Style

[ktlint](https://github.com/pinterest/ktlint) is the authority on style and is enforced on build.

Run `./gradlew ktlintFormat` to fix formatting errors in your code before commit.

## Pact Testing

Pact tests are used to test the contract between
the [Interventions UI](https://github.com/ministryofjustice/hmpps-interventions-ui) and this Service (Interventions
Service).

These are ran as part of the ci pipeline. This can be modified to run against a local branch by changing the branch name
in the [application-test.yml](./src/test/resources/application-test.yml).

By changing the `tags` field to the name of the branch we can run the pact tests against a branch to test the contract.
This branch will have had to run a CI pipeline and completed the `build` step as this publishes the files to the pact
broker.

```yaml
pactbroker:
host: pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk
scheme: https
consumerversionselectors:
tags: $BRANCH_NAME
```

## Resource documentation

- **API**: OpenAPI documentation is auto-generated. To view it start the application and visit `/swagger-ui.html` in
  your browser.
- **Data**: Data documentation is auto-generated. To view it,
  visit [https://{dev}/meta/schema](https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/meta/schema/).
  It also generates an ERD
  at [here](https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/meta/schema/relationships.html).
