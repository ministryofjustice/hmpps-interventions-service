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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ChangeLogFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
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
  inner class UpdateComplexityLevel {
    private val referral = referralFactory.createSent()
    private val user = authUserFactory.create()
    private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)
    private val complexityUUID = UUID.randomUUID()
    private val serviceCategoryUUID = UUID.randomUUID()

    @Test
    fun `updateComplexityLevel returns complexity level not updated if no referral exists`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(null)
      val complexityLevel = AmendComplexityLevelDTO(complexityUUID, "A reason for change")
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.updateComplexityLevel(serviceCategoryUUID, referral.id, complexityLevel, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("complexity level could not be updated")
    }

    @Test
    fun `updateComplexityLevel returns complexity level not updated when referral exists but fields invalid`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)
      val complexityLevel = AmendComplexityLevelDTO(UUID.randomUUID(), "A reason for change")
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.updateComplexityLevel(serviceCategoryUUID, referral.id, complexityLevel, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("complexity level could not be updated")
    }

    @Test
    fun `updateComplexityLevel updates referral complexity level with valid referral and fields `() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)
      val changelog = Changelog(referral.id, UUID.randomUUID(), AmendTopic.COMPLEXITY_LEVEL, ReferralAmendmentDetails(listOf(complexityUUID.toString())), ReferralAmendmentDetails(listOf(complexityUUID.toString())), "reason", OffsetDateTime.now(), user)
      val complexityLevel = AmendComplexityLevelDTO(complexityUUID, "A reason for change")
      whenever(amendReferralService.updateComplexityLevel(eq(referral), eq(complexityLevel), eq(serviceCategoryUUID), eq(token))).thenReturn(changelog)
      val returnedValue = amendReferralController.updateComplexityLevel(referral.id, serviceCategoryUUID, complexityLevel, token)
      assertThat(returnedValue!!.values.first()).isEqualTo(complexityLevel.complexityLevelId.toString())
    }
  }

  @Nested
  inner class AmendDesiredOutcomes {
    private val referral = referralFactory.createSent()
    private val user = authUserFactory.create()
    private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)
    private val serviceCategoryUUID = UUID.randomUUID()
    private val desiredOutcomesUUID = UUID.randomUUID()
    private val newDesiredOutcomesUUID = UUID.randomUUID()
    private val changeLogFactory = ChangeLogFactory()

    @Test
    fun `amendDesiredOutcomes do not update if no referral exists`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(null)
      val amendDesiredOutcomesDTO = AmendDesiredOutcomesDTO(listOf(newDesiredOutcomesUUID), "A reason for change")
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.amendDesiredOutcomes(token, referral.id, serviceCategoryUUID, amendDesiredOutcomesDTO)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("desired outcomes could not be updated")
    }

    @Test
    fun `amendDesiredOutcomes does not update when referral exists but fields invalid`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)
      val amendDesiredOutcomesDTO = AmendDesiredOutcomesDTO(listOf(UUID.randomUUID()), "A reason for change")
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.amendDesiredOutcomes(token, referral.id, serviceCategoryUUID, amendDesiredOutcomesDTO)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("desired outcomes could not be updated")
    }

    @Test
    fun `amendDesiredOutcomes updates referral desired outcomes for the service category`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)
      val amendDesiredOutcomesDTO = AmendDesiredOutcomesDTO(listOf(newDesiredOutcomesUUID), "A reason for change")
      val expectedChangeLog = changeLogFactory.create(
        oldVal = ReferralAmendmentDetails(listOf(desiredOutcomesUUID.toString())),
        newVal = ReferralAmendmentDetails(listOf(newDesiredOutcomesUUID.toString())),
        reasonForChange = "Amend Desired Outcomes"
      )
      whenever(
        amendReferralService.updateReferralDesiredOutcomes(
          eq(referral),
          eq(amendDesiredOutcomesDTO),
          eq(token),
          eq(serviceCategoryUUID)
        )
      ).thenReturn(expectedChangeLog)
      val returnedValue = amendReferralController.amendDesiredOutcomes(token, referral.id, serviceCategoryUUID, amendDesiredOutcomesDTO)
      assertThat(returnedValue.values).containsExactlyInAnyOrder(newDesiredOutcomesUUID.toString())
      assertThat(returnedValue.reasonForChange).isEqualTo("Amend Desired Outcomes")
    }
  }
}
