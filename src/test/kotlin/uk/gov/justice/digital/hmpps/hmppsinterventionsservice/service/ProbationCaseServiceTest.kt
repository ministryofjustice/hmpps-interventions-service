package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ProbationCaseReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime

internal class ProbationCaseServiceTest {
  private val referralRepository: ReferralRepository = mock()
  private val draftReferralRepository: DraftReferralRepository = mock()
  private val referralService: ReferralService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()
  private val deliverySessionService: DeliverySessionService = mock()
  private val deliverySessionFactory = DeliverySessionFactory()

  private val referralFactory = ReferralFactory()

  private val probationCaseService = ProbationCaseService(
    referralRepository,
    draftReferralRepository,
    referralService,
    hmppsAuthService,
    deliverySessionService,
  )

  @Test
  fun `returns correct referral information`() {
    val crn = "X123456"
    val referral = referralFactory.createAssigned(serviceUserCRN = crn)
    referralFactory.createAssigned(serviceUserCRN = "ABC123")

    val responsibleOfficer = ResponsibleProbationPractitioner(firstName = "responsible", lastName = "practitioner", email = "aa", deliusStaffCode = "aa", authUser = null)
    val referringOfficerDetails = UserDetail(firstName = "referring", lastName = "officer", email = "aaa")
    val serviceProviderUser = UserDetail(firstName = "service", lastName = "provider", email = "aaa")

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findByServiceUserCRN(crn)).thenReturn(emptyList())
    whenever(referralService.getResponsibleProbationPractitioner(referral)).thenReturn(responsibleOfficer)
    whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(referringOfficerDetails)
    whenever(hmppsAuthService.getUserDetail(referral.currentAssignee!!)).thenReturn(serviceProviderUser)

    val expectedResult = listOf(ProbationCaseReferralDTO.from(referral, responsibleOfficer, serviceProviderUser, referringOfficerDetails))

    val result = probationCaseService.getProbationCaseDetails(crn)

    assertThat(result).isNotNull
    assertThat(result).size().isEqualTo(1)
    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `returns correct referral information for multiple referrals`() {
    val crn = "X123456"
    val referral = referralFactory.createAssigned(serviceUserCRN = crn)
    val referral2 = referralFactory.createAssigned(serviceUserCRN = crn)
    val draftReferral = referralFactory.createDraft(serviceUserCRN = crn)
    val sentReferrals = listOf(referral, referral2)

    val responsibleOfficer = ResponsibleProbationPractitioner(firstName = "responsible", lastName = "practitioner", email = "aa", deliusStaffCode = "aa", authUser = null)
    val referringOfficerDetails = UserDetail(firstName = "referring", lastName = "officer", email = "aaa")
    val serviceProviderUser = UserDetail(firstName = "service", lastName = "provider", email = "aaa")

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(sentReferrals)
    whenever(referralService.getResponsibleProbationPractitioner(referral)).thenReturn(responsibleOfficer)
    whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(referringOfficerDetails)
    whenever(hmppsAuthService.getUserDetail(referral.currentAssignee!!)).thenReturn(serviceProviderUser)

    whenever(referralService.getResponsibleProbationPractitioner(referral2)).thenReturn(responsibleOfficer)
    whenever(hmppsAuthService.getUserDetail(referral2.createdBy)).thenReturn(referringOfficerDetails)
    whenever(hmppsAuthService.getUserDetail(referral2.currentAssignee!!)).thenReturn(serviceProviderUser)

    whenever(draftReferralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(draftReferral))
    whenever(hmppsAuthService.getUserDetail(draftReferral.createdBy)).thenReturn(referringOfficerDetails)

    val expectedResult = listOf(
      ProbationCaseReferralDTO.from(referral, responsibleOfficer, serviceProviderUser, referringOfficerDetails),
      ProbationCaseReferralDTO.from(referral2, responsibleOfficer, serviceProviderUser, referringOfficerDetails),
      ProbationCaseReferralDTO.Companion.from(draftReferral, referringOfficerDetails),
    )

    val result = probationCaseService.getProbationCaseDetails(crn)

    assertThat(result).isNotNull
    assertThat(result).size().isEqualTo(3)
    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `returns service provider user as null if there is no assignee`() {
    val crn = "X123456"
    val referral = referralFactory.createSent(serviceUserCRN = crn)

    val responsibleOfficer = ResponsibleProbationPractitioner(firstName = "responsible", lastName = "practitioner", email = "aa", deliusStaffCode = "aa", authUser = null)
    val referringOfficerDetails = UserDetail(firstName = "referring", lastName = "officer", email = "aaa")

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(draftReferralRepository.findByServiceUserCRN(crn)).thenReturn(emptyList())
    whenever(referralService.getResponsibleProbationPractitioner(referral)).thenReturn(responsibleOfficer)
    whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(referringOfficerDetails)

    val expectedResult = listOf(ProbationCaseReferralDTO.from(referral, responsibleOfficer, null, referringOfficerDetails))

    val result = probationCaseService.getProbationCaseDetails(crn)

    assertThat(result).isNotNull
    assertThat(result).size().isEqualTo(1)
    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `returns referral with all its appointment location information`() {
    val crn = "X123456"
    val time = OffsetDateTime.now()
    val duration = 500
    val referral = referralFactory.createAssigned(serviceUserCRN = crn)
    val referral2 = referralFactory.createAssigned(serviceUserCRN = crn)
    val sentReferrals = listOf(referral, referral2)
    val referral1Session1 = deliverySessionFactory.createScheduled(sessionNumber = 1, appointmentTime = time, durationInMinutes = duration, referral = referral)
    val referral2Session1 = deliverySessionFactory.createScheduled(sessionNumber = 1, appointmentTime = time, durationInMinutes = duration, referral = referral2)

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(sentReferrals)
    whenever(deliverySessionService.getSessions(referral.id)).thenReturn(listOf(referral1Session1))
    whenever(deliverySessionService.getSessions(referral2.id)).thenReturn(listOf(referral2Session1))
    val expectedResult = listOf(
      ReferralAppointmentLocationDetails(referral, listOf(referral1Session1.currentAppointment!!)),
      ReferralAppointmentLocationDetails(referral2, listOf(referral2Session1.currentAppointment!!)),
    )

    val result = probationCaseService.getAppointmentLocationDetails(crn)

    assertThat(result).isNotNull
    assertThat(result).size().isEqualTo(2)
    assertThat(result[0].appointments).size().isEqualTo(1)
    assertThat(result[1].appointments).size().isEqualTo(1)
    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `returns referral with multiple appointments location information`() {
    val crn = "X123456"
    val time = OffsetDateTime.now()
    val duration = 500
    val referral = referralFactory.createAssigned(serviceUserCRN = crn)
    val referral2 = referralFactory.createAssigned(serviceUserCRN = crn)
    val sentReferrals = listOf(referral, referral2)
    val referral1Session1 = deliverySessionFactory.createScheduled(sessionNumber = 1, appointmentTime = time, durationInMinutes = duration, referral = referral)
    val referral1Session2 = deliverySessionFactory.createScheduled(sessionNumber = 2, appointmentTime = time, durationInMinutes = duration, referral = referral)
    val referral2Session1 = deliverySessionFactory.createScheduled(sessionNumber = 1, appointmentTime = time, durationInMinutes = duration, referral = referral2)
    val referral2Session2 = deliverySessionFactory.createScheduled(sessionNumber = 2, appointmentTime = time, durationInMinutes = duration, referral = referral2)

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(sentReferrals)
    whenever(deliverySessionService.getSessions(referral.id)).thenReturn(listOf(referral1Session1, referral1Session2))
    whenever(deliverySessionService.getSessions(referral2.id)).thenReturn(listOf(referral2Session1, referral2Session2))
    val expectedResult = listOf(
      ReferralAppointmentLocationDetails(referral, listOf(referral1Session1.currentAppointment!!, referral1Session2.currentAppointment!!)),
      ReferralAppointmentLocationDetails(referral2, listOf(referral2Session1.currentAppointment!!, referral2Session2.currentAppointment!!)),
    )

    val result = probationCaseService.getAppointmentLocationDetails(crn)

    assertThat(result).isNotNull
    assertThat(result).size().isEqualTo(2)
    assertThat(result[0].appointments).size().isEqualTo(2)
    assertThat(result[1].appointments).size().isEqualTo(2)
    assertThat(result).isEqualTo(expectedResult)
  }
}
