package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAppointmentDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralAppointmentLocationDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentDeliveryAddressFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentDeliveryFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import java.time.OffsetDateTime
import java.util.UUID

@JsonTest
class ReferralAppointmentDetailsDTOTest(@Autowired private val json: JacksonTester<ReferralAppointmentDetailsDTO>) {
  private val deliverySessionFactory = DeliverySessionFactory()
  private val appointmentDeliveryFactory = AppointmentDeliveryFactory()
  private val appointmentDeliveryAddressFactory = AppointmentDeliveryAddressFactory()

  @Test
  fun `return referral appointments data in the request format`() {
    val id = UUID.randomUUID()
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")

    val referral = SampleData.sampleReferral(
      "X123456",
      "Provider",
      id = id,
      createdAt = createdAt,
      referenceNumber = "1234",
      accessibilityNeeds = "wheelchair",
      additionalNeedsInformation = "english",
      whenUnavailable = "tomorrow",
      endRequestedComments = "appointment needed",
    )

    val session1 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 1,
      sessionConcerns = "some concern",
      sessionSummary = "PP attended",
      sessionResponse = "It went well",
      lateReason = "Have to go the hosiptal",
      futureSessionPlan = "have to invite him again",
      appointmentTime = OffsetDateTime.parse("2024-06-30T10:41:32.767668+01:00"),
    )
    val session2 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 2,
      sessionConcerns = "some other concern",
      sessionSummary = "PP did not attended",
      sessionResponse = "It didn't go well",
      lateReason = "Have been drinking",
      futureSessionPlan = "have to invite him again",
      appointmentTime = OffsetDateTime.parse("2024-07-31T10:41:32.767668+01:00"),
    )

    val appointmentDeliveryAddress1 = appointmentDeliveryAddressFactory.create()
    val appointment1 = session1.appointments.first()
    val appointmentDelivery1 = appointmentDeliveryFactory.create(appointmentId = appointment1.id, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, npsOfficeCode = "CRSEXT")
    appointmentDelivery1.appointmentDeliveryAddress = appointmentDeliveryAddress1
    appointment1.appointmentDelivery = appointmentDelivery1

    val appointmentDeliveryAddress2 = appointmentDeliveryAddressFactory.create(
      firstAddressLine = "44 northwood boluvard",
      secondAddressLine = "castle street",
      townCity = "Warwick",
      postCode = "NH14 3GA",
      county = "Warwickshire",
    )
    val appointment2 = session2.appointments.first()
    val appointmentDelivery2 = appointmentDeliveryFactory.create(appointmentId = appointment2.id, appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL, npsOfficeCode = "CRSEXT")
    appointmentDelivery2.appointmentDeliveryAddress = appointmentDeliveryAddress2
    appointment2.appointmentDelivery = appointmentDelivery2

    referral.referenceNumber = "something"
    referral.needsInterpreter = true
    referral.interpreterLanguage = "french"
    referral.supplementaryRiskId = UUID.fromString("1c893c33-a373-435e-b13f-7efbc6206e2a")
    referral.relevantSentenceId = 123456L

    val referralAppointmentLocationDetails = ReferralAppointmentLocationDetails(referral, session1.appointments.toList() + session2.appointments.toList())

    val out = json.write(ReferralAppointmentDetailsDTO.from("X123456", listOf(referralAppointmentLocationDetails)))

    assertThat(out).isEqualToJson(
      """
        {
          "crn": "X123456",
          "referrals": [
            {
              "referral_number": "something",
              "appointments": [
                {
                  "appointment_id": ${appointment1.id},
                  "appointment_date_time": "2024-06-30T10:41:32.767668+01:00",
                  "appointment_duration_in_minutes": 120,
                  "superseded_indicator": false,
                  "appointment_delivery_first_address_line": "Harmony Living Office, Room 4",
                  "appointment_delivery_second_address_line": "44 Bouverie Road",
                  "appointment_delivery_town_city": "Blackpool",
                  "appointment_delivery_county": "Lancashire",
                  "appointment_delivery_postcode": "SY4 0RE"
                },
                {
                  "appointment_id": ${appointment2.id},
                  "appointment_date_time": "2024-07-31T10:41:32.767668+01:00",
                  "appointment_duration_in_minutes": 120,
                  "superseded_indicator": false,
                  "appointment_delivery_first_address_line": "44 northwood boluvard",
                  "appointment_delivery_second_address_line": "castle street",
                  "appointment_delivery_town_city": "Warwick",
                  "appointment_delivery_county": "Warwickshire",
                  "appointment_delivery_postcode": "NH14 3GA"
                }
              ]
            }
          ]
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `return referral appointments data when there is no appointment`() {
    val id = UUID.randomUUID()
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")

    val referral = SampleData.sampleReferral(
      "X123456",
      "Provider",
      id = id,
      createdAt = createdAt,
      referenceNumber = "1234",
      accessibilityNeeds = "wheelchair",
      additionalNeedsInformation = "english",
      whenUnavailable = "tomorrow",
      endRequestedComments = "appointment needed",
    )

    referral.referenceNumber = "something"
    referral.needsInterpreter = true
    referral.interpreterLanguage = "french"
    referral.supplementaryRiskId = UUID.fromString("1c893c33-a373-435e-b13f-7efbc6206e2a")
    referral.relevantSentenceId = 123456L

    val referralAppointmentLocationDetails = ReferralAppointmentLocationDetails(referral, emptyList())

    val out = json.write(ReferralAppointmentDetailsDTO.from("X123456", listOf(referralAppointmentLocationDetails)))

    assertThat(out).isEqualToJson(
      """
        {
          "crn": "X123456",
          "referrals": [
            {
              "referral_number": "something",
              "appointments": []
            }
          ]
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `return referral appointments data when there is no referral associated with the crn`() {
    val out = json.write(ReferralAppointmentDetailsDTO.from("X123456", emptyList()))

    assertThat(out).isEqualToJson(
      """
        {
          "crn": "X123456",
          "referrals": []
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `return referral appointments data when some of the fields are not present`() {
    val id = UUID.randomUUID()
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")

    val referral = SampleData.sampleReferral(
      "X123456",
      "Provider",
      id = id,
      createdAt = createdAt,
      referenceNumber = "1234",
      accessibilityNeeds = "wheelchair",
      additionalNeedsInformation = "english",
      whenUnavailable = "tomorrow",
      endRequestedComments = "appointment needed",
    )

    val session1 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 1,
      sessionConcerns = "some concern",
      sessionSummary = "PP attended",
      sessionResponse = "It went well",
      lateReason = "Have to go the hosiptal",
      futureSessionPlan = "have to invite him again",
      appointmentTime = OffsetDateTime.parse("2024-06-30T10:41:32.767668+01:00"),
    )
    val session2 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 2,
      sessionConcerns = "some other concern",
      sessionSummary = "PP did not attended",
      sessionResponse = "It didn't go well",
      lateReason = "Have been drinking",
      futureSessionPlan = "have to invite him again",
      appointmentTime = OffsetDateTime.parse("2024-07-31T10:41:32.767668+01:00"),
    )

    val appointmentDeliveryAddress1 = appointmentDeliveryAddressFactory.create()
    val appointment1 = session1.appointments.first()
    val appointmentDelivery1 = appointmentDeliveryFactory.create(appointmentId = appointment1.id, appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE, npsOfficeCode = "CRSEXT")
    appointmentDelivery1.appointmentDeliveryAddress = appointmentDeliveryAddress1
    appointment1.appointmentDelivery = appointmentDelivery1

    val appointmentDeliveryAddress2 = appointmentDeliveryAddressFactory.create(
      firstAddressLine = "44 northwood boluvard",
      secondAddressLine = null,
      townCity = null,
      postCode = "NH14 3GA",
      county = null,
    )
    val appointment2 = session2.appointments.first()
    val appointmentDelivery2 = appointmentDeliveryFactory.create(appointmentId = appointment2.id, appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL, npsOfficeCode = "CRSEXT")
    appointmentDelivery2.appointmentDeliveryAddress = appointmentDeliveryAddress2
    appointment2.appointmentDelivery = appointmentDelivery2

    referral.referenceNumber = "something"
    referral.needsInterpreter = true
    referral.interpreterLanguage = "french"
    referral.supplementaryRiskId = UUID.fromString("1c893c33-a373-435e-b13f-7efbc6206e2a")
    referral.relevantSentenceId = 123456L

    val referralAppointmentLocationDetails = ReferralAppointmentLocationDetails(referral, session1.appointments.toList() + session2.appointments.toList())

    val out = json.write(ReferralAppointmentDetailsDTO.from("X123456", listOf(referralAppointmentLocationDetails)))

    assertThat(out).isEqualToJson(
      """
        {
          "crn": "X123456",
          "referrals": [
            {
              "referral_number": "something",
              "appointments": [
                {
                  "appointment_id": ${appointment1.id},
                  "appointment_date_time": "2024-06-30T10:41:32.767668+01:00",
                  "appointment_duration_in_minutes": 120,
                  "superseded_indicator": false,
                  "appointment_delivery_first_address_line": "Harmony Living Office, Room 4",
                  "appointment_delivery_second_address_line": "44 Bouverie Road",
                  "appointment_delivery_town_city": "Blackpool",
                  "appointment_delivery_county": "Lancashire",
                  "appointment_delivery_postcode": "SY4 0RE"
                },
                {
                  "appointment_id": ${appointment2.id},
                  "appointment_date_time": "2024-07-31T10:41:32.767668+01:00",
                  "appointment_duration_in_minutes": 120,
                  "superseded_indicator": false,
                  "appointment_delivery_first_address_line": "44 northwood boluvard",
                  "appointment_delivery_second_address_line": "",
                  "appointment_delivery_town_city": "",
                  "appointment_delivery_county": "",
                  "appointment_delivery_postcode": "NH14 3GA"
                }
              ]
            }
          ]
        }
      """.trimIndent(),
    )
  }
}
