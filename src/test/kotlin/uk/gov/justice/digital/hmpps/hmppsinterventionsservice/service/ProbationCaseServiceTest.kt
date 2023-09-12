package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ProbationCaseReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory

internal class ProbationCaseServiceTest {
  private val referralRepository: ReferralRepository = mock()
  private val referralService: ReferralService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()

  private val referralFactory = ReferralFactory()

  private val probationCaseService = ProbationCaseService(
    referralRepository,
    referralService,
    hmppsAuthService,
  )

  @Test
  fun `returns correct referral information`() {
    val crn = "X123456"
    val referral = referralFactory.createAssigned(serviceUserCRN = crn)
    referralFactory.createAssigned(serviceUserCRN = "ABC123")

    val responsibleOfficer = ResponsibleProbationPractitioner(firstName = "responsible", lastName = "practitioner", email = "aa", deliusStaffCode = "aa", authUser = null)
    val referringOfficerDetails = UserDetail(firstName = "referring", lastName = "officer", email= "aaa")
    val serviceProviderUser = UserDetail(firstName = "service", lastName = "provider", email= "aaa")

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
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
    val referrals = listOf(referral, referral2)

    val responsibleOfficer = ResponsibleProbationPractitioner(firstName = "responsible", lastName = "practitioner", email = "aa", deliusStaffCode = "aa", authUser = null)
    val referringOfficerDetails = UserDetail(firstName = "referring", lastName = "officer", email= "aaa")
    val serviceProviderUser = UserDetail(firstName = "service", lastName = "provider", email= "aaa")

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(referrals)
    whenever(referralService.getResponsibleProbationPractitioner(referral)).thenReturn(responsibleOfficer)
    whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(referringOfficerDetails)
    whenever(hmppsAuthService.getUserDetail(referral.currentAssignee!!)).thenReturn(serviceProviderUser)

    whenever(referralService.getResponsibleProbationPractitioner(referral2)).thenReturn(responsibleOfficer)
    whenever(hmppsAuthService.getUserDetail(referral2.createdBy)).thenReturn(referringOfficerDetails)
    whenever(hmppsAuthService.getUserDetail(referral2.currentAssignee!!)).thenReturn(serviceProviderUser)

    val expectedResult = listOf(
      ProbationCaseReferralDTO.from(referral, responsibleOfficer, serviceProviderUser, referringOfficerDetails),
      ProbationCaseReferralDTO.from(referral2, responsibleOfficer, serviceProviderUser, referringOfficerDetails)
    )

    val result = probationCaseService.getProbationCaseDetails(crn)

    assertThat(result).isNotNull
    assertThat(result).size().isEqualTo(2)
    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `returns service provider user as null if there is no assignee`() {
    val crn = "X123456"
    val referral = referralFactory.createSent(serviceUserCRN = crn)

    val responsibleOfficer = ResponsibleProbationPractitioner(firstName = "responsible", lastName = "practitioner", email = "aa", deliusStaffCode = "aa", authUser = null)
    val referringOfficerDetails = UserDetail(firstName = "referring", lastName = "officer", email= "aaa")

    whenever(referralRepository.findByServiceUserCRN(crn)).thenReturn(listOf(referral))
    whenever(referralService.getResponsibleProbationPractitioner(referral)).thenReturn(responsibleOfficer)
    whenever(hmppsAuthService.getUserDetail(referral.createdBy)).thenReturn(referringOfficerDetails)

    val expectedResult = listOf(ProbationCaseReferralDTO.from(referral, responsibleOfficer, null, referringOfficerDetails))

    val result = probationCaseService.getProbationCaseDetails(crn)

    assertThat(result).isNotNull
    assertThat(result).size().isEqualTo(1)
    assertThat(result).isEqualTo(expectedResult)
  }
}
