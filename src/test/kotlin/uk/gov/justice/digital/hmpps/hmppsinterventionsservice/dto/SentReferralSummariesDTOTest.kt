package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralSummaryFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@JsonTest
class SentReferralSummariesDTOTest(@Autowired private val json: JacksonTester<SentReferralSummariesDTO>) {
  private val interventionFactory = InterventionFactory()
  private val referralSummariesFactory = ReferralSummaryFactory()

  companion object {
    @JvmStatic
    private fun locationScenarios(): Stream<Arguments> = Stream.of(
      arguments(
        PersonCurrentLocationType.CUSTODY,
        "aaa",
        null,
        null,
        null,
        """
      {
        "interventionTitle": "Accommodation Services for Yorkshire",
        "location":"aaa",
        "locationType":"CUSTODY"
      }
    """,
      ),
      arguments(
        PersonCurrentLocationType.COMMUNITY,
        null,
        "london",
        null,
        null,
        """
      {
        "interventionTitle": "Accommodation Services for Yorkshire",
        "location":"london",
        "locationType":"COMMUNITY"
      }
    """,
      ),
      arguments(
        PersonCurrentLocationType.COMMUNITY,
        null,
        null,
        "hackney",
        null,
        """
      {
        "interventionTitle": "Accommodation Services for Yorkshire",
        "location":"hackney",
        "locationType":"COMMUNITY"
      }
    """,
      ),
      arguments(
        PersonCurrentLocationType.COMMUNITY,
        null,
        null,
        null,
        "nottingham",
        """
      {
        "interventionTitle": "Accommodation Services for Yorkshire",
        "location":"nottingham",
        "locationType":"COMMUNITY"
      }
    """,
      ),
      arguments(
        null,
        null,
        null,
        null,
        null,
        """
      {
        "interventionTitle": "Accommodation Services for Yorkshire",
      }
    """,
      ),
    )
  }

  @Test
  fun `sent referral includes all fields related to referral Summary`() {
    val id = UUID.fromString("3B9ED289-8412-41A9-8291-45E33E60276C")
    val sentAt = OffsetDateTime.parse("2021-01-13T21:57:13+00:00")
    val sentBy = AuthUser("id", "source", "username")
    val serviceUserData = ServiceUserData(
      "Mr",
      "Jamie",
      "Smith",
      dateOfBirth = LocalDate.of(1981, 7, 20),
      gender = "male",
      ethnicity = "Indian",
    )

    val referralSummary = referralSummariesFactory.createSent(
      id = id,
      serviceUserData = serviceUserData,
      referenceNumber = "something",
      sentAt = sentAt,
      sentBy = sentBy,
    )

    val out = json.write(SentReferralSummariesDTO.from(referralSummary))
    println("the json is ===>$out")
    Assertions.assertThat(out).isEqualToJson(
      """
      {
        "id" : "$id",
        "sentAt": "2021-01-13T21:57:13Z",
        "sentBy": {
          "username": "username",
          "authSource": "source",
          "userId":"id",
        },
        "referenceNumber": "something",
        "serviceUser": {"crn": "X123456"},
        "serviceProvider": {id:"HARMONY_LIVING","name": "Harmony Living"}
      }
    """,
    )
  }

  @Test
  fun `sent referral DTO includes intervention title`() {
    val id = UUID.fromString("3B9ED289-8412-41A9-8291-45E33E60276C")
    val sentAt = OffsetDateTime.parse("2021-01-13T21:57:13+00:00")
    val sentBy = AuthUser("id", "source", "username")
    val serviceUserData = ServiceUserData(
      "Mr",
      "Jamie",
      "Smith",
      dateOfBirth = LocalDate.of(1981, 7, 20),
      gender = "male",
      ethnicity = "Indian",
    )

    val sentReferralSummary = referralSummariesFactory.createSent(
      id = id,
      serviceUserData = serviceUserData,
      referenceNumber = "something",
      intervention = interventionFactory.create(title = "Accommodation Services for Yorkshire"),
      sentAt = sentAt,
      sentBy = sentBy,
    )
    val out = json.write(SentReferralSummariesDTO.from(sentReferralSummary))
    Assertions.assertThat(out).isEqualToJson(
      """
      {
        "interventionTitle": "Accommodation Services for Yorkshire"
      }
    """,
    )
  }

  @ParameterizedTest
  @MethodSource("locationScenarios")
  fun `sent referral DTO includes the right location`(
    personCurrentLocationType: PersonCurrentLocationType?,
    prisonId: String?,
    probationOffice: String?,
    pdu: String?,
    nDeliusPDU: String?,
    expectedJson: String?,
  ) {
    val id = UUID.fromString("3B9ED289-8412-41A9-8291-45E33E60276C")
    val sentAt = OffsetDateTime.parse("2021-01-13T21:57:13+00:00")
    val sentBy = AuthUser("id", "source", "username")
    val serviceUserData = ServiceUserData(
      "Mr",
      "Jamie",
      "Smith",
      dateOfBirth = LocalDate.of(1981, 7, 20),
      gender = "male",
      ethnicity = "Indian",
    )

    val sentReferralSummary = referralSummariesFactory.createSentReferralSummaryWithReferral(
      id = id,
      serviceUserData = serviceUserData,
      referenceNumber = "something",
      intervention = interventionFactory.create(title = "Accommodation Services for Yorkshire"),
      sentAt = sentAt,
      sentBy = sentBy,
      type = personCurrentLocationType,
      prisonId = prisonId,
      probationOffice = probationOffice,
      pdu = pdu,
      nDeliusPDU = nDeliusPDU,
    )

    val out = json.write(SentReferralSummariesDTO.from(sentReferralSummary.first))
    Assertions.assertThat(out).isEqualToJson(expectedJson)
  }
}
