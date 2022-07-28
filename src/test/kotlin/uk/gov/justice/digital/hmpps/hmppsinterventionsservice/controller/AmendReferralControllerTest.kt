package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedAndRequirementDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.util.UUID

internal class AmendReferralControllerTest {
  private val amendReferralService = mock<AmendReferralService>()
  private val amendReferralController = AmendReferralController(
    amendReferralService
  )
  private val tokenFactory = JwtTokenFactory()
  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()

  @Nested
  inner class GetSentReferralForUser {
    private val referral = referralFactory.createSent()
    private val user = authUserFactory.create()
    private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)
    private val complexityUUID = UUID.randomUUID()
    private val serviceCategoryUUID = UUID.randomUUID()

    @Test
    fun `updateComplexityLevel returns complexity level not updated if no referral exists`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(null)
      val complexityLevel = AmendComplexityLevelDTO(complexityUUID, serviceCategoryUUID, "A reason for change")
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.updateComplexityLevel(referral.id, complexityLevel, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("complexity level could not be updated")
    }

    @Test
    fun `updateComplexityLevel returns complexity level not updated when referral exists but fields invalid`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)
      val complexityLevel = AmendComplexityLevelDTO(UUID.randomUUID(), UUID.randomUUID(), "A reason for change")
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.updateComplexityLevel(referral.id, complexityLevel, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("complexity level could not be updated")
    }

    @Test
    fun `updateComplexityLevel updates referral complexity level with valid referral and fields `() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)
      val complexityLevel = AmendComplexityLevelDTO(complexityUUID, serviceCategoryUUID, "A reason for change")
      whenever(amendReferralService.updateComplexityLevel(eq(referral), eq(complexityLevel), eq(token))).thenReturn(complexityLevel)
      val returnedValue = amendReferralController.updateComplexityLevel(referral.id, complexityLevel, token)
      assertThat(returnedValue?.complexityLevelId).isEqualTo(complexityLevel.complexityLevelId)
      assertThat(returnedValue?.serviceCategoryId).isEqualTo(complexityLevel.serviceCategoryId)
    }

    @Test
    fun `updateNeedsAndRequirement returns needs and requirements not updated if no referral exists`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(null)

      val needsAndRequirements = AmendNeedAndRequirementDTO(
        "",
        "am here now",
        "",
        true,
        "yoruba",
        false,
        "",
        "A reason for change"
      )
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.updateNeedsAndRequirements(referral.id, needsAndRequirements, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("needs and requirements could not be updated")
    }

    @Test
    fun `updateNeedsAndRequirement update needs and requirements with valid referral and fields `() {
      val referral = referralFactory.createSent()
      referral.additionalNeedsInformation = "not going good"
      referral.accessibilityNeeds = "good"
      referral.needsInterpreter = false
      referral.interpreterLanguage = "Spanish"
      referral.hasAdditionalResponsibilities = true
      referral.whenUnavailable = ""
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)
      var accessibilityNeeds = "enough for today"
      val oldAccessibilityNeeds = referral.accessibilityNeeds
      val needsAndRequirements = AmendNeedAndRequirementDTO(
        "",
        "am here now",
        accessibilityNeeds,
        true,
        "Yoruba",
        false,
        "",
        "A reason for change"
      )
      referral.accessibilityNeeds = accessibilityNeeds
      whenever(amendReferralService.updateNeedAndRequirement(eq(referral), eq(needsAndRequirements), eq(token))).thenReturn(needsAndRequirements)
      val returnedValue = amendReferralController.updateNeedsAndRequirements(referral.id, needsAndRequirements, token)
      assertThat(returnedValue?.accessibilityNeeds).isEqualTo(accessibilityNeeds)
      assertThat(referral.accessibilityNeeds).isNotEqualTo(oldAccessibilityNeeds)
      assertThat(returnedValue?.furtherInformation).isEqualTo(needsAndRequirements.furtherInformation)
    }
  }
}
