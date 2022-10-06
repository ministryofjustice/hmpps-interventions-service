package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDetailsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.notifications.ReferralNotificationService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AssignmentsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralDetailsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class NotifyReferralServiceTest {
  private val emailSender = mock<EmailSender>()
  private val hmppsAuthService = mock<HMPPSAuthService>()
  private val authUserRepository = mock<AuthUserRepository>()
  private val referralService = mock<ReferralService>()
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()
  private val assignmentsFactory = AssignmentsFactory()

  private val referralSentEvent = ReferralEvent(
    "source",
    ReferralEventType.SENT,
    referralFactory.createSent(
      id = UUID.fromString("68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"),
      referenceNumber = "JS8762AC"
    ),
    "http://localhost:8080/sent-referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"
  )

  private val referralAssignedEvent = ReferralEvent(
    "source",
    ReferralEventType.ASSIGNED,
    referralFactory.createSent(
      id = UUID.fromString("42C7D267-0776-4272-A8E8-A673BFE30D0D"),
      referenceNumber = "AJ9871AC",
      assignments = assignmentsFactory.createInOrder(authUserFactory.createSP())
    ),
    "http://localhost:8080/sent-referral/42c7d267-0776-4272-a8e8-a673bfe30d0d"
  )

  private fun notifyService(): ReferralNotificationService {
    return ReferralNotificationService(
      "complexityLevelTemplateID",
      "desiredOutcomesAmendTemplateID",
      "needsAndRequirementsAmendTemplateID",
      "referralSentTemplateID",
      "referralAssignedTemplateID",
      "completionDeadlineUpdatedTemplateID",
      "enforceableDaysUpdatedTemplateID",
      "http://interventions-ui.example.com",
      "/referral/{id}",
      emailSender,
      hmppsAuthService,
      authUserRepository,
      referralService
    )
  }

  @Test
  fun `referral sent event generates valid url and sends an email`() {
    notifyService().onApplicationEvent(referralSentEvent)
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(
      eq("referralSentTemplateID"),
      eq("shs-incoming@provider.example.com"),
      personalisationCaptor.capture()
    )
    assertThat(personalisationCaptor.firstValue["organisationName"]).isEqualTo("Harmony Living")
    assertThat(personalisationCaptor.firstValue["referenceNumber"]).isEqualTo("JS8762AC")
    assertThat(personalisationCaptor.firstValue["referralUrl"]).isEqualTo("http://interventions-ui.example.com/referral/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080")
  }

  @Test
  fun `referral assigned event does not send email when user details are not available`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUserDTO>())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> {
      notifyService().onApplicationEvent(referralAssignedEvent)
    }
    verifyNoInteractions(emailSender)
  }

  @Test
  fun `referral assigned event does not swallows hmpps auth errors`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenThrow(UnverifiedEmailException::class.java)
    assertThrows<UnverifiedEmailException> { notifyService().onApplicationEvent(referralAssignedEvent) }
  }

  @Test
  fun `referral assigned event generates valid url and sends an email`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("tom", "tom@tom.tom", "jones"))

    notifyService().onApplicationEvent(referralAssignedEvent)
    val personalisationCaptor = argumentCaptor<Map<String, String>>()
    verify(emailSender).sendEmail(eq("referralAssignedTemplateID"), eq("tom@tom.tom"), personalisationCaptor.capture())
    assertThat(personalisationCaptor.firstValue["spFirstName"]).isEqualTo("tom")
    assertThat(personalisationCaptor.firstValue["referenceNumber"]).isEqualTo("AJ9871AC")
    assertThat(personalisationCaptor.firstValue["referralUrl"]).isEqualTo("http://interventions-ui.example.com/referral/42c7d267-0776-4272-a8e8-a673bfe30d0d")
  }

  @Nested
  inner class ReferralDetailsChangedEvent {
    private val referralDetailsFactory = ReferralDetailsFactory()
    private val referral = referralFactory.createAssigned()

    private val makeReferralDetailsCompletionDeadlineChangedEvent = { assigned: Boolean ->
      ReferralEvent(
        "source",
        ReferralEventType.DETAILS_AMENDED,
        referral,
        "http://localhost:8080/sent-referral/${referral.id}",
        data = mapOf(
          "newDetails" to ReferralDetailsDTO.from(
            referralDetailsFactory.create(
              referral.id,
              referral.createdAt,
              referral.createdBy,
              completionDeadline = LocalDate.of(2022, 6, 6)
            )
          ),
          "previousDetails" to ReferralDetailsDTO.from(
            referralDetailsFactory.create(
              referral.id,
              referral.createdAt,
              referral.createdBy,
              completionDeadline = LocalDate.of(2022, 4, 13)
            )
          ),
          "currentAssignee" to if (assigned) AuthUserDTO.from(referral.currentAssignee!!) else null,
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy
        )
      )
    }
    private val makeReferralDetailsEnforceableDaysChangedEvent = { assigned: Boolean ->
      ReferralEvent(
        "source",
        ReferralEventType.DETAILS_AMENDED,
        referral,
        "http://localhost:8080/sent-referral/${referral.id}",
        data = mapOf(
          "newDetails" to ReferralDetailsDTO.from(
            referralDetailsFactory.create(
              referral.id,
              referral.createdAt,
              referral.createdBy,
              maximumNumberOfEnforceableDays = 2
            )
          ),
          "previousDetails" to ReferralDetailsDTO.from(
            referralDetailsFactory.create(
              referral.id,
              referral.createdAt,
              referral.createdBy,
              maximumNumberOfEnforceableDays = 3
            )
          ),
          "currentAssignee" to if (assigned) AuthUserDTO.from(referral.currentAssignee!!) else null,
          "crn" to referral.serviceUserCRN,
          "sentBy" to referral.sentBy,
          "createdBy" to referral.createdBy
        )
      )
    }

    @Test
    fun `amending 'completion deadline' notifies the assigned caseworker via email`() {
      whenever(authUserRepository.findById(referral.createdBy.id)).thenReturn(Optional.of(referral.createdBy))
      whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(
        UserDetail(
          "sally",
          "sally@tom.com",
          "smith"
        )
      )
      whenever(hmppsAuthService.getUserDetail(AuthUserDTO.from(referral.currentAssignee!!))).thenReturn(
        UserDetail(
          "tom",
          "tom@tom.tom",
          "jones"
        )
      )
      whenever(referralService.getResponsibleProbationPractitioner(any(), any(), any())).thenReturn(
        ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def")
      )
      whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(true)

      notifyService().onApplicationEvent(makeReferralDetailsCompletionDeadlineChangedEvent(true))
      val personalisationCaptor = argumentCaptor<Map<String, String>>()
      verify(emailSender).sendEmail(
        eq("completionDeadlineUpdatedTemplateID"),
        eq("tom@tom.tom"),
        personalisationCaptor.capture()
      )
      assertThat(personalisationCaptor.firstValue["caseworkerFirstName"]).isEqualTo("tom")
      assertThat(personalisationCaptor.firstValue["newCompletionDeadline"]).isEqualTo("6 June 2022")
      assertThat(personalisationCaptor.firstValue["previousCompletionDeadline"]).isEqualTo("13 April 2022")
      assertThat(personalisationCaptor.firstValue["changedByName"]).isEqualTo("sally smith")
      assertThat(personalisationCaptor.firstValue["referralDetailsUrl"]).isEqualTo("http://interventions-ui.example.com/referral/${referral.id}")

      // reason for change contains sensitive information
      assertThat(personalisationCaptor.firstValue["reasonForChange"]).isNull()
    }

    @Test
    fun `amending 'completion deadline' notifies the responsible officer via email`() {
      whenever(authUserRepository.findById(referral.createdBy.id)).thenReturn(Optional.of(referral.createdBy))
      whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(
        UserDetail(
          "sally",
          "sally@tom.com",
          "smith"
        )
      )
      whenever(hmppsAuthService.getUserDetail(AuthUserDTO.from(referral.currentAssignee!!))).thenReturn(
        UserDetail(
          "tom",
          "tom@tom.tom",
          "jones"
        )
      )
      whenever(referralService.getResponsibleProbationPractitioner(any(), any(), any())).thenReturn(
        ResponsibleProbationPractitioner("abc", "abc@abc.com", null, null, "def")
      )
      whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(false)

      notifyService().onApplicationEvent(makeReferralDetailsCompletionDeadlineChangedEvent(true))
      val personalisationCaptor = argumentCaptor<Map<String, String>>()
      verify(emailSender).sendEmail(
        eq("completionDeadlineUpdatedTemplateID"),
        eq("tom@tom.tom"),
        personalisationCaptor.capture()
      )
      verify(emailSender).sendEmail(
        eq("completionDeadlineUpdatedTemplateID"),
        eq("abc@abc.com"),
        personalisationCaptor.capture()
      )
      assertThat(personalisationCaptor.firstValue["caseworkerFirstName"]).isEqualTo("tom")
      assertThat(personalisationCaptor.firstValue["newCompletionDeadline"]).isEqualTo("6 June 2022")
      assertThat(personalisationCaptor.firstValue["previousCompletionDeadline"]).isEqualTo("13 April 2022")
      assertThat(personalisationCaptor.firstValue["changedByName"]).isEqualTo("sally smith")
      assertThat(personalisationCaptor.firstValue["referralDetailsUrl"]).isEqualTo("http://interventions-ui.example.com/referral/${referral.id}")

      // reason for change contains sensitive information
      assertThat(personalisationCaptor.firstValue["reasonForChange"]).isNull()
    }

    @Test
    fun `amending 'completion deadline' does not send email for unassigned referrals`() {
      notifyService().onApplicationEvent(makeReferralDetailsCompletionDeadlineChangedEvent(false))
      verifyNoInteractions(emailSender)
    }

    @Test
    fun `amending 'enforceable days' notifies the assigned caseworker via email`() {
      whenever(authUserRepository.findById(referral.createdBy.id)).thenReturn(Optional.of(referral.createdBy))
      whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(
        UserDetail(
          "sally",
          "sally@tom.com",
          "smith"
        )
      )
      whenever(hmppsAuthService.getUserDetail(AuthUserDTO.from(referral.currentAssignee!!))).thenReturn(
        UserDetail(
          "tom",
          "tom@tom.tom",
          "jones"
        )
      )

      notifyService().onApplicationEvent(makeReferralDetailsEnforceableDaysChangedEvent(true))
      val personalisationCaptor = argumentCaptor<Map<String, String>>()
      verify(emailSender).sendEmail(
        eq("enforceableDaysUpdatedTemplateID"),
        eq("tom@tom.tom"),
        personalisationCaptor.capture()
      )
      assertThat(personalisationCaptor.firstValue["recipientFirstName"]).isEqualTo("tom")
      assertThat(personalisationCaptor.firstValue["newMaximumEnforceableDays"]).isEqualTo("2")
      assertThat(personalisationCaptor.firstValue["previousMaximumEnforceableDays"]).isEqualTo("3")
      assertThat(personalisationCaptor.firstValue["changedByName"]).isEqualTo("sally smith")
      assertThat(personalisationCaptor.firstValue["referralDetailsUrl"]).isEqualTo("http://interventions-ui.example.com/referral/${referral.id}")

      // reason for change contains sensitive information
      assertThat(personalisationCaptor.firstValue["reasonForChange"]).isNull()
    }

    @Test
    fun `amending 'enforceable days' does not send email for unassigned referrals`() {
      notifyService().onApplicationEvent(makeReferralDetailsEnforceableDaysChangedEvent(false))
      verifyNoInteractions(emailSender)
    }

    private val caseworker: AuthUser = authUserFactory.createSP("SPUser", "sp_caseworker")

    private fun makeReferralChangedEvent(eventType: ReferralEventType, assigned: Boolean): ReferralEvent {
      val referral = if (assigned) {
        referralFactory.createAssigned(assignments = assignmentsFactory.createInOrder(caseworker))
      } else {
        referralFactory.createSent()
      }

      return ReferralEvent(
        "source",
        eventType,
        referral,
        "http://localhost:8080/sent-referral/${referral.id}"
      )
    }

    @Test
    fun `amending 'desired outcome' notifies the assigned caseworker via email`() {
      whenever(hmppsAuthService.getUserDetail(caseworker)).thenReturn(
        UserDetail(firstName = "Outcome", email = "outcome.caseworker@provider.example.org", lastName = "unused")
      )

      val event = makeReferralChangedEvent(ReferralEventType.DESIRED_OUTCOMES_AMENDED, assigned = true)
      notifyService().onApplicationEvent(event)

      verify(emailSender).sendEmail(
        templateID = "desiredOutcomesAmendTemplateID",
        emailAddress = "outcome.caseworker@provider.example.org",
        mapOf(
          "sp_first_name" to "Outcome",
          "referral_number" to event.referral.referenceNumber!!,
          "referral" to "http://interventions-ui.example.com/referral/${event.referral.id}"
        )
      )
    }

    @Test
    fun `amending 'desired outcome' does not send email for unassigned referrals`() {
      notifyService().onApplicationEvent(
        makeReferralChangedEvent(ReferralEventType.DESIRED_OUTCOMES_AMENDED, assigned = false)
      )
      verifyNoInteractions(emailSender)
    }

    @Test
    fun `amending 'complexity level' notifies the assigned caseworker via email`() {
      whenever(hmppsAuthService.getUserDetail(caseworker)).thenReturn(
        UserDetail(firstName = "Complexity", email = "complexity.caseworker@provider.example.org", lastName = "unused")
      )

      val event = makeReferralChangedEvent(ReferralEventType.COMPLEXITY_LEVEL_AMENDED, assigned = true)
      notifyService().onApplicationEvent(event)

      verify(emailSender).sendEmail(
        templateID = "complexityLevelTemplateID",
        emailAddress = "complexity.caseworker@provider.example.org",
        mapOf(
          "sp_first_name" to "Complexity",
          "referral_number" to event.referral.referenceNumber!!,
          "referral" to "http://interventions-ui.example.com/referral/${event.referral.id}"
        )
      )
    }

    @Test
    fun `amending 'complexity level' does not send email for unassigned referrals`() {
      notifyService().onApplicationEvent(
        makeReferralChangedEvent(ReferralEventType.COMPLEXITY_LEVEL_AMENDED, assigned = false)
      )
      verifyNoInteractions(emailSender)
    }

    @Test
    fun `amending 'needs and requirements' notifies the assigned caseworker via email`() {
      whenever(hmppsAuthService.getUserDetail(caseworker)).thenReturn(
        UserDetail(firstName = "Needs", email = "needs.caseworker@provider.example.org", lastName = "unused")
      )

      val event = makeReferralChangedEvent(ReferralEventType.NEEDS_AND_REQUIREMENTS_AMENDED, assigned = true)
      notifyService().onApplicationEvent(event)

      verify(emailSender).sendEmail(
        templateID = "needsAndRequirementsAmendTemplateID",
        emailAddress = "needs.caseworker@provider.example.org",
        mapOf(
          "sp_first_name" to "Needs",
          "referral_number" to event.referral.referenceNumber!!,
          "referral" to "http://interventions-ui.example.com/referral/${event.referral.id}"
        )
      )
    }

    @Test
    fun `amending 'needs and requirements' does not send email for unassigned referrals`() {
      notifyService().onApplicationEvent(
        makeReferralChangedEvent(ReferralEventType.NEEDS_AND_REQUIREMENTS_AMENDED, assigned = false)
      )
      verifyNoInteractions(emailSender)
    }
  }
}
