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
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ChangelogValuesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
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
    fun `getChangelog returns multiple changelog entries`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)

      val changeLogValuesList = mutableListOf<ChangelogValuesDTO>(
        ChangelogValuesDTO(UUID.randomUUID(), referral.id, "COMPLEXITY_LEVEL", OffsetDateTime.now(), "PP's Name", "A reason"),
        ChangelogValuesDTO(UUID.randomUUID(), referral.id, "COMPLEXITY_LEVEL", OffsetDateTime.now(), "PP's Name 2", "Another reason")
      )
      whenever(amendReferralService.getListOfChangeLogEntries(eq(referral))).thenReturn(changeLogValuesList)

      val returnedChangeLogObject = amendReferralController.getChangelog(referral.id, token)

      assertThat(returnedChangeLogObject?.size).isEqualTo(2)
      assertThat(returnedChangeLogObject?.get(0)?.changelogId).isEqualTo(changeLogValuesList.get(0).changelogId)
      assertThat(returnedChangeLogObject?.get(0)?.referralId).isEqualTo(changeLogValuesList.get(0).referralId)
    }

    @Test
    fun `getChangelog returns no changelog entries`() {
      whenever(amendReferralService.getSentReferralForAuthenticatedUser(eq(token), eq(referral.id))).thenReturn(referral)

      val changeLogValuesList = mutableListOf<ChangelogValuesDTO>()
      whenever(amendReferralService.getListOfChangeLogEntries(eq(referral))).thenReturn(changeLogValuesList)

      val returnedChangeLogObject = amendReferralController.getChangelog(referral.id, token)

      assertThat(returnedChangeLogObject?.size).isEqualTo(0)
    }
  }
}
