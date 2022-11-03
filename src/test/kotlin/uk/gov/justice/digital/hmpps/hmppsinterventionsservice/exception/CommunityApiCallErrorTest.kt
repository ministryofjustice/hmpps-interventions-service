package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.GONE
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE

internal class CommunityApiCallErrorTest {
  data class Example(
    val response: HttpStatus,
    val causeMessage: String,
    val expectedCategory: String,
    val expectedUserMessage: String
  )

  @Test
  fun `maps incoming community-api errors into normalised categories and user messages`() {
    // TODO: user message resolution does not belong to a system-facing API; give 'translatable' codes instead
    val examples = listOf(
      Example(
        response = BAD_REQUEST,
        causeMessage = "Contact type 'CRSSAA' requires an outcome type as the contact date is in the past '2021-06-29'",
        expectedCategory = "Contact type 'CRSSAA' requires an outcome type as the contact date is in the past '_-_-_'",
        expectedUserMessage = "Delius requires an appointment to only be made for today or in the future. Please amend and try again",
      ),
      Example(
        response = BAD_REQUEST,
        causeMessage = "Validation failure: startTime endTime must be after or equal to startTime, endTime endTime must be after or equal to startTime",
        expectedCategory = "Validation failure: startTime endTime must be after or equal to startTime, endTime endTime must be after or equal to startTime",
        expectedUserMessage = "Delius requires that an appointment must end on the same day it starts. Please amend and try again",
      ),
      Example(
        response = BAD_REQUEST,
        causeMessage = "CRN: E376578 EventId: 1502929161 has multiple referral requirements",
        expectedCategory = "CRN: _ EventId: _ has multiple referral requirements",
        expectedUserMessage = "The Service User Sentence must not contain more than one active rehabilitation activity requirement. Please correct in Delius and try again",
      ),
      Example(
        response = BAD_REQUEST,
        causeMessage = "Cannot find NSI for CRN: M190420 Sentence: 1503032340 and ContractType ACC",
        expectedCategory = "Cannot find NSI for CRN: _ Sentence: _ and ContractType ACC",
        expectedUserMessage = "This update has not been possible. This is because there's been a change to the referral in non-structured interventions in nDelius. Ask the probation practitioner if anything has changed with the referral or the person on probation",
      ),
      Example(
        response = BAD_REQUEST,
        causeMessage = "Multiple existing URN NSIs found for referral: e2eb5e02-6e6f-4c34-8be9-b0525ea78c5a, NSI IDs: [3182407, 3182408]",
        expectedCategory = "Multiple existing URN NSIs found for referral: _, NSI IDs: [_, _]",
        expectedUserMessage = "There has been an error during creating the Delius NSI. We are aware of this issue. For follow-up, please contact support",
      ),
      Example(
        response = BAD_REQUEST,
        causeMessage = "Multiple existing matching NSIs found for referral: e2eb5e02-6e6f-4c34-8be9-b0525ea78c5a, NSI IDs: [3182407, 3182408]",
        expectedCategory = "Multiple existing matching NSIs found for referral: _, NSI IDs: [_, _]",
        expectedUserMessage = "There has been an error during creating the Delius NSI. We are aware of this issue. For follow-up, please contact support",
      ),
      Example(
        response = CONFLICT,
        causeMessage = "409 Conflict from POST https://community-api-secure.probation.service.justice.gov.uk/secure/offenders/crn/E188275/sentence/1503090023/appointments/context/commissioned-rehabilitation-services",
        expectedCategory = "_ Conflict from POST https://community-api-secure.probation.service.justice.gov.uk/secure/offenders/crn/_/sentence/_/appointments/context/commissioned-rehabilitation-services",
        expectedUserMessage = "The appointment conflicts with another. Please contact the service user's probation practitioner for an available slot and try again",
      ),
      Example(
        response = CONFLICT,
        causeMessage = "409 Conflict from POST https://community-api-secure.probation.service.justice.gov.uk/secure/offenders/crn/D889766/appointments/1761440075/reschedule/context/commissioned-rehabilitation-services",
        expectedCategory = "_ Conflict from POST https://community-api-secure.probation.service.justice.gov.uk/secure/offenders/crn/_/appointments/_/reschedule/context/commissioned-rehabilitation-services",
        expectedUserMessage = "The appointment conflicts with another. Please contact the service user's probation practitioner for an available slot and try again",
      ),
      Example(
        response = GONE,
        causeMessage = "An unrecognised issue 'A123' and 234566",
        expectedCategory = "An unrecognised issue '_' and _",
        expectedUserMessage = "Delius reported \"An unrecognised issue 'A123' and 234566\". Please correct, if possible, otherwise contact support",
      ),
      Example(
        response = SERVICE_UNAVAILABLE,
        causeMessage = "Any message",
        expectedCategory = "Any message",
        expectedUserMessage = "System is experiencing issues. Please try again later and if the issue persists contact Support",
      ),
    )

    examples.forEach { example ->
      val error = CommunityApiCallError(example.response, example.causeMessage, "{}", RuntimeException())

      assertThat(error.userMessage).isEqualTo(example.expectedUserMessage)
      assertThat(error.category).isEqualTo(example.expectedCategory)
    }
  }
}
