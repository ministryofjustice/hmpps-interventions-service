# How to Update SAR Tests

The SAR (Subject Access Request) tests verify that the API data, controller behaviour,
service logic, and mustache template endpoint all work correctly.

## Test classes

| Class | Location | Purpose |
|-------|----------|---------|
| `SarsDataControllerTest` | `src/test/kotlin/.../controller/SarsDataControllerTest.kt` | Unit tests for `SarsDataController` – covers API responses for valid/missing CRNs and the template endpoint |
| `SarDataDTOTest` | `src/test/kotlin/.../controller/SarDataDTOTest.kt` | JSON serialisation tests for `SarDataDTO`, including referrals, appointments, action plans, end-of-service reports and case notes |
| `SarsDataServiceTest` | `src/test/kotlin/.../service/SarsDataServiceTest.kt` | Repository-backed tests for `SarsDataService` – covers data retrieval, date-range filtering and draft referral handling |
| `SubjectAccessRequestTemplateIntegrationTest` | `src/test/kotlin/.../integration/sar/SubjectAccessRequestTemplateIntegrationTest.kt` | Integration tests for `GET /subject-access-request/template` – verifies auth, role checks and mustache template content |

## When do you need to update the tests?

Update the tests whenever any of the following change:

- The SAR data returned by the service (e.g. new fields added to a DTO) → update `SarDataDTOTest`
- The `SarsDataController` behaviour (e.g. new endpoints or response codes) → update `SarsDataControllerTest`
- The `SarsDataService` data retrieval logic → update `SarsDataServiceTest`
- The mustache template structure or the `/subject-access-request/template` endpoint → update `SubjectAccessRequestTemplateIntegrationTest`

## Running the SAR tests

Run all SAR-related tests:

```bash
./gradlew test --tests "*Sar*" --tests "*SubjectAccessRequest*"
```

Or run a specific test class:

```bash
./gradlew test --tests "*SarsDataControllerTest*"
./gradlew test --tests "*SarDataDTOTest*"
./gradlew test --tests "*SarsDataServiceTest*"
./gradlew test --tests "*SubjectAccessRequestTemplateIntegrationTest*"
```

## What each test checks

### `SarsDataControllerTest`

Unit tests for `SarsDataController`. Covers:

- `GET /subject-access-request?crn=<crn>` returns 200 with referral data
- Returns 209 when no CRN is supplied
- Returns 204 when no data exists for the CRN
- `GET /subject-access-request/template` returns the mustache template content

### `SarDataDTOTest`

JSON serialisation tests for `SarDataDTO`. Verifies the exact JSON shape of the API
response under various scenarios (full data, missing optional fields, no referrals,
draft referrals only).

If the response structure changes, update the expected JSON in these tests.

### `SarsDataServiceTest`

Repository-backed integration tests for `SarsDataService`. Covers:

- Fetching referrals and appointments by CRN
- Filtering by date range
- Handling CRNs with no matching referrals
- Distinguishing true draft referrals from sent referrals

### `SubjectAccessRequestTemplateIntegrationTest`

Full Spring integration tests for the template endpoint. Verifies:

- Authenticated requests with `ROLE_SAR_DATA_ACCESS` receive the mustache template
- Requests without a token return 401
- Requests with an insufficient role return 403
- The returned template contains expected mustache markers (e.g. `{{crn}}`, `{{#referral}}`)

