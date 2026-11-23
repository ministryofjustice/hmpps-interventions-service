# CircleCI to GitHub Actions migration

Follows the [HMPPS migration guide](https://tech-docs.hmpps.service.justice.gov.uk/shared-tooling/migrating-to-GHA/).
`.circleci/config.yml` has been removed; the original is in git history.

## What changed

| CircleCI | GitHub Actions |
|---|---|
| `validate` | `pipeline.yml` › `validate` (Postgres 15 service, Flyway, `./gradlew check jacocoTestCoverageVerification`, reports uploaded as artifact) |
| `validate_db` | `pipeline.yml` › `validate_db` (`git reset --hard` replaced by `git checkout --force`; needs `fetch-depth: 0` so `origin/main` exists) |
| `validate_helm` (server dry run) | `pipeline.yml` › `helm_lint` (shared `test_helm_lint.yml`, dev/preprod/prod matrix). No cluster credentials needed, but it no longer does a server-side dry run |
| `build_docker` / `build_multiplatform_docker` | `pipeline.yml` › `build` (shared `docker_build.yml`, multi-platform, pushes only on `main` and `hotfix/*`) |
| `publish_data` | `pipeline.yml` › `publish_data`. Uses `app_version` from `build` instead of `mem/recall` |
| `deploy_env` dev → preprod → prod | `deploy_dev` → `deploy_preprod` → `deploy_prod` (shared `deploy_to_env.yml`) |
| `approve_prod` | Required reviewers on the `prod` GitHub environment |
| `tag_pact_version` ×3 | `tag-pact-version.yml` (reusable), called after each deploy |
| `pact` workflow / `only_pacts` | `pact.yml` (runs on push and `workflow_dispatch` with `consumer_tags`) |
| Flyway install repeated 4× | `.github/actions/setup-flyway` composite action |

Other changes:
- `helm_deploy/values-*.yaml`: `quay.io/hmpps/...` → `ghcr.io/ministryofjustice/...`. The data dictionary image uses the same `image.repository`, so it moves too.
- README: CircleCI badge replaced by pipeline badge.
- Actions are pinned to commit SHAs. `hmpps-github-actions` workflows use `@v2`, as in the HMPPS templates.

## Data dictionary
`publish_data` migrates a Postgres service, runs `script/build-data-dictionary.sh` (SchemaSpy + `script/Dockerfile.data-dictionary`),
tags the image `ghcr.io/ministryofjustice/hmpps-interventions-service:data-dictionary-<app_version>` and pushes it on `main`/`hotfix/*` only.
`deploy_dev` needs it, as in CircleCI. The chart pulls `{{ image.repository }}:data-dictionary-{{ image.tag }}`, so the app and
dictionary images must share the repository path.

## Deviations to be aware of
- **Image tag:** the shared deploy action sets `generic-service.image.tag`, but this chart uses top-level `image.tag`, so `helm_additional_args: --set image.tag=<version>` is passed.
- **Helm release name:** the shared action uses the repo name by default, matching the old release name. `show_changelog` is off because there is no deployment with that name (the Kubernetes deployment is `hmpps-interventions-service-api`).
- **Registry:** the current Kotlin template pushes to ECR; the tech docs say ghcr. This uses ghcr. Confirm with #ask-hmpps-digital-sre.
- **Preprod/prod on branches:** `deploy_dev` only runs on `main`/`hotfix/*`; later deploys depend on it, so they are skipped elsewhere.
- **Pact on every push:** previously the `pact` workflow ran on push too (CircleCI default), so this is unchanged.
- **Runner size:** CircleCI `large` is replaced by `ubuntu-latest`; `_JAVA_OPTIONS` kept. Switch to a larger runner if the tests are slow.
- **Slack:** the shared workflow uses `NONPROD_RELEASES_SLACK_CHANNEL` / `PROD_RELEASES_SLACK_CHANNEL` variables and `HMPPS_SRE_SLACK_BOT_TOKEN`; the old `alerts-slack-channel` / `releases-slack-channel` parameters are not used.

## Before merging
1. Cloud Platform: add the `cloud-platform-terraform-hmpps-template` module for `dev`, `preprod`, `prod` in cloud-platform-environments (environment names must match `values-*.yaml`; set `reviewer_teams` for prod).
2. Repo secret `PACTBROKER_AUTH_PASSWORD` (was in CircleCI context `hmpps-common-vars`); Slack variables above if notifications are wanted.
3. Check the ghcr package visibility allows the cluster to pull it.
4. Repoint the Pact Broker webhook from CircleCI (`only_pacts`) to a GitHub `workflow_dispatch` on `pact.yml`.
5. Test on a branch first (no push or deploy happens off `main`/`hotfix/*`).

## Cutover (after the first successful `main` run)
- Delete webhook `https://circleci.com/hooks/github` in repo settings.
- Branch protection: replace `ci/circleci:*` checks with the GitHub ones (`Validate`, `Validate database migrations`, `Helm lint (*)`, `Build docker image`).
- "Stop building" in CircleCI; delete project env vars and unused contexts (`hmpps-common-vars`, `hmpps-interventions-*-deploy`).
