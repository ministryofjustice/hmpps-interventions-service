package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.pact

import au.com.dius.pact.provider.junitsupport.State
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AddressDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.SetupAssistant
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanActivity
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import java.time.OffsetDateTime
import java.util.UUID

class ActionPlanContracts(private val setupAssistant: SetupAssistant) {
  @State("an action plan with ID 81987e8b-aeb9-4fbf-8ecb-1a054ad74b2d exists with 1 appointment with recorded attendance")
  fun `create an empty action plan with 1 appointment that has had attendance recorded`() {
    val referral = setupAssistant.createSentReferral()
    setupAssistant.createActionPlan(id = UUID.fromString("81987e8b-aeb9-4fbf-8ecb-1a054ad74b2d"), numberOfSessions = 1, referral = referral)
    setupAssistant.createDeliverySession(
      1,
      120,
      OffsetDateTime.parse("2021-05-13T13:30:00+01:00"),
      Attended.LATE,
      "Alex missed the bus",
      referral = referral,
    )
  }

  @State(
    "an action plan with ID e5ed2f80-dfe2-4bf3-b5c4-d8d4486e963d exists and it has 3 scheduled appointments",
    "an action plan with ID e5ed2f80-dfe2-4bf3-b5c4-d8d4486e963d exists and has an appointment for session 1",
  )
  fun `create a submitted action plan with 3 appointments`() {
    val referral = setupAssistant.createSentReferral()
    setupAssistant.createActionPlan(id = UUID.fromString("e5ed2f80-dfe2-4bf3-b5c4-d8d4486e963d"), numberOfSessions = 3, referral = referral)
    setupAssistant.createDeliverySession(1, 120, OffsetDateTime.parse("2021-05-13T13:30:00+01:00"), appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL, referral = referral)
    setupAssistant.createDeliverySession(2, 120, OffsetDateTime.parse("2021-05-20T13:30:00+01:00"), appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL, referral = referral)
    setupAssistant.createDeliverySession(3, 120, OffsetDateTime.parse("2021-05-27T13:30:00+01:00"), appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL, referral = referral)
  }

  @State(
    "an action plan with ID 345059d4-1697-467b-8914-fedec9957279 exists and has 2 2-hour appointments already",
    "an action plan with ID 345059d4-1697-467b-8914-fedec9957279 exists and has an appointment for which no session feedback has been recorded",
  )
  fun `create an empty draft plan with 2 2 hours appointments`() {
    val referral = setupAssistant.createSentReferral(sentAt = OffsetDateTime.now().minusDays(1))
    setupAssistant.createActionPlan(
      id = UUID.fromString("345059d4-1697-467b-8914-fedec9957279"),
      numberOfSessions = 2,
      referral = referral,
    )
    val appointmentDeliveryAddress = AddressDTO(
      firstAddressLine = "Harmony Living Office, Room 4",
      secondAddressLine = "44 Bouverie Road",
      townOrCity = "Blackpool",
      county = "Lancashire",
      postCode = "SY40RE",
    )
    setupAssistant.createDeliverySession(
      1,
      120,
      OffsetDateTime.now(),
      appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER,
      appointmentDeliveryAddress = appointmentDeliveryAddress,
      appointmentSessionType = AppointmentSessionType.GROUP,
      referral = referral,
    )
    setupAssistant.createDeliverySession(
      2,
      120,
      OffsetDateTime.now(),
      appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_OTHER,
      appointmentDeliveryAddress = appointmentDeliveryAddress,
      appointmentSessionType = AppointmentSessionType.GROUP,
      referral = referral,
    )
  }

  @State("an action plan exists with ID 7a165933-d851-48c1-9ab0-ff5b8da12695, and it has been submitted")
  fun `create an action plan that has been submitted`() {
    setupAssistant.createActionPlan(
      id = UUID.fromString("7a165933-d851-48c1-9ab0-ff5b8da12695"),
      submittedBy = setupAssistant.createSPUser(),
      submittedAt = OffsetDateTime.now(),
      numberOfSessions = 1,
      activities = mutableListOf(
        ActionPlanActivity(
          description = "Attend training course",
        ),
      ),
    )
  }

  @State("an action plan exists with ID f3ade2c5-075a-4235-9826-eed289e4d17a, and it has been approved")
  fun `create an action plan that has been approved`() {
    setupAssistant.createActionPlan(
      id = UUID.fromString("f3ade2c5-075a-4235-9826-eed289e4d17a"),
      submittedBy = setupAssistant.createSPUser(),
      submittedAt = OffsetDateTime.now(),
      approvedBy = setupAssistant.createPPUser(),
      approvedAt = OffsetDateTime.now(),
      activities = mutableListOf(
        ActionPlanActivity(
          description = "Attend training course",
        ),
      ),
    )
  }

  @State("There is an existing sent referral with ID of 8b423e17-9b60-4cc2-a927-8941ac76fdf9, and it has an action plan")
  fun `create sent referral 8b423e17 with action plan`() {
    val referral = setupAssistant.createSentReferral(id = UUID.fromString("8b423e17-9b60-4cc2-a927-8941ac76fdf9"))
    setupAssistant.createActionPlan(referral = referral)
  }

  @State("an action plan exists with ID dfb64747-f658-40e0-a827-87b4b0bdcfed, and it has not been submitted")
  fun `create a an draft plan with activities and has not been submitted`() {
    setupAssistant.createActionPlan(id = UUID.fromString("dfb64747-f658-40e0-a827-87b4b0bdcfed"))
  }

  @State("a caseworker has been assigned to a sent referral and an action plan can be created")
  fun `create a sent referral with the id specified in the contract`() {
    setupAssistant.createSentReferral(id = UUID.fromString("81d754aa-d868-4347-9c0f-50690773014e"))
  }

  @State("an action plan exists with id dfb64747-f658-40e0-a827-87b4b0bdcfed")
  fun `create a an empty draft plan`() {
    setupAssistant.createActionPlan(id = UUID.fromString("dfb64747-f658-40e0-a827-87b4b0bdcfed"))
  }

  @State(
    "a draft action plan with ID 6e8dfb5c-127f-46ea-9846-f82b5fd60d27 exists and is ready to be submitted",
    "a draft action plan with ID 6e8dfb5c-127f-46ea-9846-f82b5fd60d27 exists and it has an activity with ID fd1b6653-ea7b-4e12-9d45-72ff9b1a3ea0",
  )
  fun `create a an draft plan with activities`() {
    setupAssistant.createActionPlan(
      id = UUID.fromString("6e8dfb5c-127f-46ea-9846-f82b5fd60d27"),
      numberOfSessions = 4,
      activities = mutableListOf(
        ActionPlanActivity(
          id = UUID.fromString("fd1b6653-ea7b-4e12-9d45-72ff9b1a3ea0"),
          description = "Attend training course",
        ),
      ),
    )
  }

  @State("an action plan with ID 0f5afe04-e323-4699-9423-fb6122580638 exists with 1 appointment with recorded attendance and behaviour")
  fun `create an action plan with an appointment with recorded attendance and behaviour`() {
    val referral = setupAssistant.createSentReferral()
    setupAssistant.createActionPlan(
      id = UUID.fromString("0f5afe04-e323-4699-9423-fb6122580638"),
      numberOfSessions = 1,
      referral = referral,
    )

    setupAssistant.createDeliverySession(
      1,
      120,
      OffsetDateTime.parse("2021-05-13T13:30:00+01:00"),
      Attended.LATE,
      "Alex missed the bus",
      behaviour = "Alex was well behaved",
      notifyPPOfBehaviour = false,
      referral = referral,
    )
  }

  @State("two approved action plans exists with IDs f3ade2c5-075a-4235-9826-eed289e4d17a and 8f3e1895-9c46-40ad-bdb3-d33eacbb693e")
  fun `create two action plans for a referral`() {
    val referralId = UUID.fromString("8d107952-9bde-4854-ad1e-dee09daab992")
    val actionPlan1Id = UUID.fromString("f3ade2c5-075a-4235-9826-eed289e4d17")
    val actionPlan2Id = UUID.fromString("8f3e1895-9c46-40ad-bdb3-d33eacbb693e")

    val referral = setupAssistant.createSentReferral(id = referralId)

    setupAssistant.createActionPlan(id = actionPlan1Id, referral = referral)
    setupAssistant.createActionPlan(id = actionPlan2Id, referral = referral)
  }
}
