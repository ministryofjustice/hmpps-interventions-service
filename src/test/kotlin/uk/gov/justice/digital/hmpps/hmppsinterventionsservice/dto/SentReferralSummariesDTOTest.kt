package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SentReferralSummariesFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonTest
class SentReferralSummariesDTOTest(@Autowired private val json: JacksonTester<SentReferralSummariesDTO>) {
  private val interventionFactory = InterventionFactory()
  private val sentReferralSummariesFactory = SentReferralSummariesFactory()

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
      ethnicity = "Indian"
    )

    val sentReferralSummary = sentReferralSummariesFactory.createSent(
      id = id,
      serviceUserData = serviceUserData,
      referenceNumber = "something",
      sentAt = sentAt,
      sentBy = sentBy
    )

    val out = json.write(SentReferralSummariesDTO.from(sentReferralSummary))
    Assertions.assertThat(out).isEqualToJson(
      """
      {
        "sentAt": "2021-01-13T21:57:13Z",
        "sentBy": {
          "username": "username",
          "authSource": "source"
        },
        "referenceNumber": "something",
        "serviceUser": {"crn": "X123456"},
        "serviceProvider": {id:"HARMONY_LIVING","name": "Harmony Living"}
      }
    """
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
      ethnicity = "Indian"
    )

    val sentReferralSummary = sentReferralSummariesFactory.createSent(
      id = id,
      serviceUserData = serviceUserData,
      referenceNumber = "something",
      intervention = interventionFactory.create(title = "Accommodation Services for Yorkshire"),
      sentAt = sentAt,
      sentBy = sentBy
    )
    val out = json.write(SentReferralSummariesDTO.from(sentReferralSummary))
    Assertions.assertThat(out).isEqualToJson(
      """
      {
        "interventionTitle": "Accommodation Services for Yorkshire"
      }
    """
    )
  }
}
