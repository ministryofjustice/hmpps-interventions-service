package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.EmailSender
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.CreateCaseNoteEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.notifications.CaseNotesNotificationsService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CaseNoteFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceUserFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class CaseNotesNotificationsServiceTest {
  private val referralService = mock<ReferralService>()
  private val emailSender = mock<EmailSender>()
  private val hmppsAuthService = mock<HMPPSAuthService>()

  private val caseNotesNotificationsService = CaseNotesNotificationsService(
    "sent-template",
    "sent-template-pp",
    "https://interventions.gov.uk",
    "/service-provider/case-note/{id}",
    "/probation-practitioner/case-note/{id}",
    emailSender,
    referralService,
    hmppsAuthService,
  )

  private val authUserFactory = AuthUserFactory()
  private val referralFactory = ReferralFactory()
  private val caseNoteFactory = CaseNoteFactory()
  private val serviceUserFactory = ServiceUserFactory()

  @Test
  fun `both PPs (responsible officer) and SPs (assignee) get notifications for a sent case note as long as they didn't send it`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("sp", "sp@provider.co.uk", "last"))
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(
      ResponsibleProbationPractitioner("pp", "pp@justice.gov.uk", null, null, "last"),
    )
    whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(false)

    val sender = authUserFactory.createSP(id = "sp_sender", userName = "sp_user_name")
    val referral = referralFactory.createAssigned()
    referral.serviceUserData = ReferralServiceUserData(firstName = "sp", lastName = "bob")
    val caseNote = caseNoteFactory.create(referral = referral, sentBy = sender, subject = "from sp", body = "body")
    whenever(referralService.getSentReferralForUser(referral.id, sender)).thenReturn(referral)
    val eventRequiringSPNotification = CreateCaseNoteEvent(
      "source",
      caseNote.id,
      caseNote.sentBy,
      "detailUrl",
      referral.id,
      true,
    )

    caseNotesNotificationsService.onApplicationEvent(eventRequiringSPNotification)

    verify(emailSender, times(2)).sendEmail(any(), any(), any())
    verify(emailSender, times(1)).sendEmail(
      "sent-template",
      "sp@provider.co.uk",
      mapOf(
        "recipientFirstName" to "sp",
        "referralNumber" to caseNote.referral.referenceNumber!!,
        "caseNoteUrl" to "https://interventions.gov.uk/service-provider/case-note/${caseNote.id}",
        "popFullName" to "Sp Bob",
      ),
    )
    verify(emailSender, times(1)).sendEmail(
      "sent-template-pp",
      "pp@justice.gov.uk",
      mapOf(
        "pp_first_name" to "pp",
        "referralNumber" to caseNote.referral.referenceNumber!!,
        "caseNoteUrl" to "https://interventions.gov.uk/probation-practitioner/case-note/${caseNote.id}",
        "popFullName" to "Sp Bob",
        "crn" to referral.serviceUserCRN,
      ),
    )
  }

  @Test
  fun `PP (responsible officer) does not get notified if they sent the case note`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("sp", "sp@provider.co.uk", "last"))
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(
      ResponsibleProbationPractitioner("pp", "pp@justice.gov.uk", "N01UTAA", null, "last"),
    )
    whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(true)

    val sender = authUserFactory.createPP(id = "pp_sender")
    val referral = referralFactory.createAssigned()
    referral.serviceUserData = ReferralServiceUserData(firstName = "sp", lastName = "bob")
    val caseNote = caseNoteFactory.create(referral = referral, sentBy = sender, subject = "from pp", body = "body")
    whenever(referralService.getSentReferralForUser(referral.id, sender)).thenReturn(referral)
    val eventRequiringSPNotification = CreateCaseNoteEvent(
      "source",
      caseNote.id,
      caseNote.sentBy,
      "detailUrl",
      referral.id,
      true,
    )

    caseNotesNotificationsService.onApplicationEvent(eventRequiringSPNotification)

    // only called once
    verify(emailSender, times(1)).sendEmail(any(), any(), any())
    // with sp details
    verify(emailSender).sendEmail(
      "sent-template",
      "sp@provider.co.uk",
      mapOf(
        "recipientFirstName" to "sp",
        "referralNumber" to caseNote.referral.referenceNumber!!,
        "caseNoteUrl" to "https://interventions.gov.uk/service-provider/case-note/${caseNote.id}",
        "popFullName" to "Sp Bob",
      ),
    )
  }

  @Test
  fun `PP (referring officer) does not get notified if they sent the case note`() {
    val sender = authUserFactory.createPP(id = "pp_sender")

    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("sp", "sp@provider.co.uk", "last"))
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(
      ResponsibleProbationPractitioner("pp", "pp@justice.gov.uk", null, sender, "last"),
    )
    whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(true)

    val referral = referralFactory.createAssigned()
    referral.serviceUserData = ReferralServiceUserData(firstName = "sp", lastName = "bob")
    val caseNote = caseNoteFactory.create(referral = referral, sentBy = sender, subject = "from pp", body = "body")
    whenever(referralService.getSentReferralForUser(referral.id, sender)).thenReturn(referral)
    val eventRequiringSPNotification = CreateCaseNoteEvent(
      "source",
      caseNote.id,
      caseNote.sentBy,
      "detailUrl",
      referral.id,
      true,
    )

    caseNotesNotificationsService.onApplicationEvent(eventRequiringSPNotification)

    // only called once
    verify(emailSender, times(1)).sendEmail(any(), any(), any())
    // with sp details
    verify(emailSender).sendEmail(
      "sent-template",
      "sp@provider.co.uk",
      mapOf(
        "recipientFirstName" to "sp",
        "referralNumber" to caseNote.referral.referenceNumber!!,
        "caseNoteUrl" to "https://interventions.gov.uk/service-provider/case-note/${caseNote.id}",
        "popFullName" to "Sp Bob",
      ),
    )
  }

  @Test
  fun `SP (assignee) does not get notified if they sent the case note`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("sp", "sp@provider.co.uk", "last"))
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(
      ResponsibleProbationPractitioner("pp", "pp@justice.gov.uk", null, null, "last"),
    )
    whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(false)

    val sender = authUserFactory.createSP(id = "sp_sender")
    val referral = referralFactory.createAssigned(assignments = listOf(ReferralAssignment(OffsetDateTime.now(), sender, sender)))
    referral.serviceUserData = ReferralServiceUserData(firstName = "sp", lastName = "bob")
    val caseNote = caseNoteFactory.create(referral = referral, sentBy = sender, subject = "from sp", body = "body")
    whenever(referralService.getSentReferralForUser(referral.id, sender)).thenReturn(referral)
    val eventRequiringSPNotification = CreateCaseNoteEvent(
      "source",
      caseNote.id,
      caseNote.sentBy,
      "detailUrl",
      referral.id,
      true,
    )

    caseNotesNotificationsService.onApplicationEvent(eventRequiringSPNotification)

    // only called once
    verify(emailSender, times(1)).sendEmail(any(), any(), any())
    // with pp details
    verify(emailSender).sendEmail(
      "sent-template-pp",
      "pp@justice.gov.uk",
      mapOf(
        "pp_first_name" to "pp",
        "referralNumber" to caseNote.referral.referenceNumber!!,
        "caseNoteUrl" to "https://interventions.gov.uk/probation-practitioner/case-note/${caseNote.id}",
        "popFullName" to "Sp Bob",
        "crn" to referral.serviceUserCRN,
      ),
    )
  }

  // This is not a normal scenario. Has only arisen as hmpps-auth has returned the same user with
  // two different ids and now username is used in checking if two authUser records represent the same
  // user
  @Test
  fun `SP (assignee) does not get notified as the assignee has the same user name as the sender`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("sp", "sp@provider.co.uk", "last"))
    val responsiblePp = authUserFactory.createSP(id = "responsiblePp", userName = "sameUserName")
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(
      ResponsibleProbationPractitioner("pp", "pp@justice.gov.uk", null, responsiblePp, "last"),
    )
    whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(false)

    val assignedToId = authUserFactory.createSP(id = "sp_assignedToId", userName = "sameUserName")
    val referral = referralFactory.createAssigned(assignments = listOf(ReferralAssignment(OffsetDateTime.now(), assignedToId, assignedToId)))
    referral.serviceUserData = ReferralServiceUserData(firstName = "sp", lastName = "bob")

    val sender = authUserFactory.createSP(id = "sp_sender", userName = "sameUserName")
    val caseNote = caseNoteFactory.create(referral = referral, sentBy = sender, subject = "from sp", body = "body")
    whenever(referralService.getSentReferralForUser(referral.id, sender)).thenReturn(referral)
    val eventRequiringSPNotification = CreateCaseNoteEvent(
      "source",
      caseNote.id,
      caseNote.sentBy,
      "detailUrl",
      referral.id,
      true,
    )

    caseNotesNotificationsService.onApplicationEvent(eventRequiringSPNotification)

    // only called once
    verify(emailSender, times(1)).sendEmail(any(), any(), any())
    // with pp details
    verify(emailSender).sendEmail(
      "sent-template-pp",
      "pp@justice.gov.uk",
      mapOf(
        "pp_first_name" to "pp",
        "referralNumber" to caseNote.referral.referenceNumber!!,
        "caseNoteUrl" to "https://interventions.gov.uk/probation-practitioner/case-note/${caseNote.id}",
        "popFullName" to "Sp Bob",
        "crn" to referral.serviceUserCRN,
      ),
    )
  }

  @Test
  fun `SP (assignee) does not get notified if the referral is unassigned`() {
    whenever(hmppsAuthService.getUserDetail(any<AuthUser>())).thenReturn(UserDetail("sp", "sp@provider.co.uk", "last"))
    whenever(referralService.getResponsibleProbationPractitioner(any())).thenReturn(
      ResponsibleProbationPractitioner("pp", "pp@justice.gov.uk", null, null, "last"),
    )
    whenever(referralService.isUserTheResponsibleOfficer(any(), any())).thenReturn(false)

    val sender = authUserFactory.createPP(id = "sender")
    val referral = referralFactory.createSent()
    referral.serviceUserData = ReferralServiceUserData(firstName = "sp", lastName = "bob")
    val caseNote = caseNoteFactory.create(referral = referral, sentBy = sender, subject = "from pp", body = "body")
    whenever(referralService.getSentReferralForUser(caseNote.referral.id, sender)).thenReturn(caseNote.referral)
    val event = CreateCaseNoteEvent(
      "source",
      caseNote.id,
      caseNote.sentBy,
      "detailUrl",
      caseNote.referral.id,
      true,
    )

    caseNotesNotificationsService.onApplicationEvent(event)

    // only called once
    verify(emailSender, times(1)).sendEmail(any(), any(), any())
    // with pp details
    verify(emailSender).sendEmail(
      "sent-template-pp",
      "pp@justice.gov.uk",
      mapOf(
        "pp_first_name" to "pp",
        "referralNumber" to caseNote.referral.referenceNumber!!,
        "caseNoteUrl" to "https://interventions.gov.uk/probation-practitioner/case-note/${caseNote.id}",
        "popFullName" to "Sp Bob",
        "crn" to referral.serviceUserCRN,
      ),
    )
  }

  @Test
  fun `exception thrown if no referral found`() {
    val sender = authUserFactory.createPP(id = "sender")
    val caseNote = caseNoteFactory.create(sentBy = sender, subject = "from pp", body = "body")
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(null)
    val event = CreateCaseNoteEvent(
      "source",
      caseNote.id,
      caseNote.sentBy,
      "detailUrl",
      UUID.fromString("e5274827-fcbc-4891-a68b-d9b4d3b59dab"),
      true,
    )
    var e = assertThrows<RuntimeException> { caseNotesNotificationsService.onApplicationEvent(event) }
    Assertions.assertThat(e.message).contains("Unable to retrieve referral for id e5274827-fcbc-4891-a68b-d9b4d3b59dab")
  }
}
