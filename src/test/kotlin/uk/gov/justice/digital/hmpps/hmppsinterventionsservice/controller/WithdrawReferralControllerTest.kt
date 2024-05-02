package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.WithdrawReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.WithdrawalReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralWithdrawalState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.util.UUID

class WithdrawReferralControllerTest {

  private val referralService = mock<ReferralService>()
  private val referralConcluder = mock<ReferralConcluder>()
  private val authUserRepository = mock<AuthUserRepository>()
  private val userMapper = UserMapper(authUserRepository)

  private val withdrawReferralController = WithdrawReferralController(referralService, userMapper, referralConcluder)

  private val tokenFactory = JwtTokenFactory()
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()

  @Test
  fun `get all withdrawal reasons`() {
    val withdrawalReasons = listOf(
      WithdrawalReason(code = "aaa", description = "reason 1", grouping = "a"),
      WithdrawalReason(code = "bbb", description = "reason 2", grouping = "b"),
    )
    whenever(referralService.getWithdrawalReasons()).thenReturn(withdrawalReasons)
    val response = withdrawReferralController.getWithdrawalReasons()
    assertThat(response).isEqualTo(response)
  }

  @Test
  fun `successfully call withdraw referral`() {
    val referral = referralFactory.createSent()
    val withdrawReferralRequestDTO = WithdrawReferralRequestDTO("AAA", "comment", ReferralWithdrawalState.PRE_ICA_WITHDRAWAL.name)

    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    val user = AuthUser("CRN123", "auth", "user")
    val token = tokenFactory.create(user.id, user.authSource, user.userName)
    val endedReferral = referralFactory.createEnded(endRequestedComments = "comment")
    whenever(referralService.requestReferralEnd(any(), any(), any())).thenReturn(endedReferral)
    whenever(referralConcluder.requiresEndOfServiceReportCreation(endedReferral)).thenReturn(true)
    whenever(authUserRepository.save(any())).thenReturn(user)

    withdrawReferralController.submitWithdrawalReasons(referral.id, withdrawReferralRequestDTO, token)
    verify(referralService).requestReferralEnd(referral, user, withdrawReferralRequestDTO)
  }

  @Test
  fun `end referral endpoint does not find referral`() {
    val withdrawReferralRequestDTO = WithdrawReferralRequestDTO("AAA", "comment", ReferralWithdrawalState.PRE_ICA_WITHDRAWAL.name)

    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(null)
    whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())

    val token = tokenFactory.create()
    val e = assertThrows<ResponseStatusException> {
      withdrawReferralController.submitWithdrawalReasons(UUID.randomUUID(), withdrawReferralRequestDTO, token)
    }
    assertThat(e.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `is set after ending a referral`() {
    val referral = referralFactory.createSent()
    val withdrawReferralRequestDTO = WithdrawReferralRequestDTO("AAA", "comment", ReferralWithdrawalState.PRE_ICA_WITHDRAWAL.name)

    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

    val user = AuthUser("CRN123", "auth", "user")
    val token = tokenFactory.create(user.id, user.authSource, user.userName)
    val endedReferral = referralFactory.createEnded(endRequestedComments = "comment")
    whenever(referralService.requestReferralEnd(any(), any(), any())).thenReturn(endedReferral)
    whenever(referralConcluder.requiresEndOfServiceReportCreation(referral)).thenReturn(true)
    whenever(authUserRepository.save(any())).thenReturn(user)

    val response = withdrawReferralController.submitWithdrawalReasons(referral.id, withdrawReferralRequestDTO, token)
  }
}
