# HMPPS Interventions Service

[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/swagger-ui.html)

[Open PRs](https://github.com/ministryofjustice/hmpps-interventions-service/pulls)

Business/domain API to **find, arrange and monitor an intervention** for service users (offenders).

## Quickstart

Please see the [quickstart document](doc/quickstart.md).

For documentation about configuring one-off or scheduled jobs, see [the components document](doc/components.md).

Common tasks and runbooks are in [the service's technical guidance](https://ministryofjustice.github.io/hmpps-interventions-docs/runbooks/).

## Architecture

To see where this service fits in the broader interventions (and probation) architecture, you can browse the HMPPS C4 models [here](https://structurizr.com/share/56937/diagrams#interventions-container).

- [Decisions specific to this application](doc/adr)
- [Decisions relevant to the "interventions" domain](https://github.com/ministryofjustice/hmpps-interventions-docs)

## Code Style

[ktlint](https://github.com/pinterest/ktlint) is the authority on style and is enforced on build.

Run `./gradlew ktlintFormat` to fix formatting errors in your code before commit.

## Resource documentation

- **API**: OpenAPI documentation is auto-generated. To view it start the application and visit `/swagger-ui.html` in your browser.
- **Data**: Data documentation is auto-generated. To view it, visit [https://{dev}/meta/schema](https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/meta/schema/).
  It also generates an ERD at [here](https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk/meta/schema/relationships.html).
