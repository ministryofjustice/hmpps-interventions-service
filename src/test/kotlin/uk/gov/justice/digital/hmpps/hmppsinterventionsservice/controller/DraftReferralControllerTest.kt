package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateReferralRequestDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DraftOasysRiskInformationService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.DraftReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcluder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.util.UUID
import javax.persistence.EntityNotFoundException

internal class DraftReferralControllerTest {
  private val draftReferralService = mock<DraftReferralService>()
  private val referralConcluder = mock<ReferralConcluder>()
  private val authUserRepository = mock<AuthUserRepository>()
  private val userMapper = UserMapper(authUserRepository)
  private val draftOasysRiskInformationService = mock<DraftOasysRiskInformationService>()
  private val draftReferralController = DraftReferralController(
    draftReferralService,
    referralConcluder,
    userMapper,
    draftOasysRiskInformationService,
  )
  private val tokenFactory = JwtTokenFactory()
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()

  @Test
  fun `createDraftReferral handles EntityNotFound exceptions from InterventionsService`() {
    val token = tokenFactory.create()
    whenever(draftReferralService.createDraftReferral(any(), any(), any(), anyOrNull(), anyOrNull())).thenThrow(
      EntityNotFoundException::class.java,
    )
    whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())
    assertThrows<ServerWebInputException> {
      draftReferralController.createDraftReferral(CreateReferralRequestDTO("CRN20", UUID.randomUUID()), token)
    }
  }

  @Nested
  inner class GetSentReferralForUser {
    private val user = authUserFactory.create()

    @Nested
    inner class SetsEndOfServiceReportRequired {
      private val user = authUserFactory.create()
      private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)

      @Test
      fun `is set after sending a referral`() {
        val request = MockHttpServletRequest()
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        val draftReferral = referralFactory.createDraft()
        val sentReferral = referralFactory.createSent()
        whenever(draftReferralService.getDraftReferralForUser(draftReferral.id, user)).thenReturn(draftReferral)
        whenever(draftReferralService.sendDraftReferral(draftReferral, user)).thenReturn(sentReferral)
        whenever(referralConcluder.requiresEndOfServiceReportCreation(sentReferral)).thenReturn(false)
        whenever(authUserRepository.save(any())).thenReturn(user)
        val sentReferralResponse = draftReferralController.sendDraftReferral(
          draftReferral.id,
          token,
        )
        assertThat(sentReferralResponse.body.id).isEqualTo(sentReferral.id)
        assertThat(sentReferralResponse.body.endOfServiceReportCreationRequired).isFalse
      }
    }

    @Nested
    inner class GetDraftReferralByID {
      private val referral = referralFactory.createDraft()
      private val user = authUserFactory.create()
      private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)
      private val clientApiToken = tokenFactory.create("interventions-event-client", "ROLE_INTERVENTIONS_API_READ_ALL")

      @Test
      fun `getDraftReferralByID returns a sent referral if it exists`() {
        whenever(draftReferralService.getDraftReferralForUser(eq(referral.id), any())).thenReturn(referral)
        whenever(authUserRepository.save(any())).thenReturn(user)

        val draftReferral = draftReferralController.getDraftReferralByID(
          referral.id,
          token,
        )
        assertThat(draftReferral.id).isEqualTo(referral.id)
      }
    }

    @Nested
    inner class GetDraftReferrals {
      private val referral = referralFactory.createDraft()
      private val user = authUserFactory.create()
      private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)

      @Test
      fun `getDraftReferrals returns a list of draft referrals if they exist`() {
        whenever(draftReferralService.getDraftReferralsForUser(any())).thenReturn(listOf(referral))
        whenever(authUserRepository.save(any())).thenReturn(user)

        val draftReferrals = draftReferralController.getDraftReferrals(token)
        assertThat(draftReferrals).isNotNull
        assertThat(draftReferrals.count()).isEqualTo(1)
      }
    }
  }
}
