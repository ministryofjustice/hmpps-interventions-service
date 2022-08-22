package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.UserDetail
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ChangeLogFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class AmendReferralControllerTest {
  private val amendReferralService = mock<AmendReferralService>()
  private val hmppsAuthService = mock<HMPPSAuthService>()
  private val amendReferralController = AmendReferralController(
    amendReferralService,
    hmppsAuthService
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
      val complexityLevel = AmendComplexityLevelDTO(complexityUUID, "A reason for change")
      val serverWebInputException = ServerWebInputException("sent referral not found [id=${referral.id}]")
      whenever(amendReferralService.updateComplexityLevel(any(), any(), any(), any()))
        .thenThrow(serverWebInputException)
      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.updateComplexityLevel(serviceCategoryUUID, referral.id, complexityLevel, token)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("sent referral not found [id=${referral.id}]")
    }

    @Test
    fun `updateComplexityLevel updates referral complexity level with valid referral and fields `() {
      val complexityLevel = AmendComplexityLevelDTO(complexityUUID, "A reason for change")
      doNothing().whenever(amendReferralService)
        .updateComplexityLevel(eq(referral.id), eq(complexityLevel), eq(serviceCategoryUUID), eq(token))
      val returnedValue =
        amendReferralController.updateComplexityLevel(referral.id, serviceCategoryUUID, complexityLevel, token)
      assertThat(returnedValue.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
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
      val amendDesiredOutcomesDTO = AmendDesiredOutcomesDTO(listOf(newDesiredOutcomesUUID), "A reason for change")
      val serverWebInputException = ServerWebInputException("sent referral not found [id=${referral.id}]")
      whenever(amendReferralService.updateReferralDesiredOutcomes(any(), any(), any(), any()))
        .thenThrow(serverWebInputException)

      val exception = assertThrows<ResponseStatusException> {
        amendReferralController.amendDesiredOutcomes(token, referral.id, serviceCategoryUUID, amendDesiredOutcomesDTO)
      }
      assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(exception.reason).isEqualTo("sent referral not found [id=${referral.id}]")
    }

    @Test
    fun `amendDesiredOutcomes updates referral desired outcomes for the service category`() {
      val amendDesiredOutcomesDTO = AmendDesiredOutcomesDTO(listOf(newDesiredOutcomesUUID), "A reason for change")
      doNothing().whenever(amendReferralService)
        .updateReferralDesiredOutcomes(eq(referral.id), eq(amendDesiredOutcomesDTO), eq(token), eq(serviceCategoryUUID))
      val returnedValue =
        amendReferralController.amendDesiredOutcomes(token, referral.id, serviceCategoryUUID, amendDesiredOutcomesDTO)
      assertThat(returnedValue.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
  }

  @Nested
  inner class AmendNeedsAndRequirements {
    private val referral = referralFactory.createSent()
    private val user = authUserFactory.create()
    private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)

    @Test
    fun `amendNeedsAndRequirements updates details in referral for the correct type`() {
      val amendNeedsAndRequirementsDTO = AmendNeedsAndRequirementsDTO(true, "9-12AM", "A reason for change")
      doNothing().whenever(amendReferralService)
        .updateAmendCaringOrEmploymentResponsibilitiesDTO(eq(referral.id), eq(amendNeedsAndRequirementsDTO), eq(token))
      val returnedValue =
        amendReferralController.amendNeedsAndRequirements(token, referral.id, "additional-responsibilities", amendNeedsAndRequirementsDTO)
      assertThat(returnedValue.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `amendNeedsAndRequirements returns bad request for incorrect correct type for amendReferralService updateAmendAccessibilityNeedsDTO`() {
      val amendNeedsAndRequirementsDTO = AmendNeedsAndRequirementsDTO(true, "9-12AM", "A reason for change")
      doNothing().whenever(amendReferralService)
        .updateAmendAccessibilityNeedsDTO(eq(referral.id), eq(amendNeedsAndRequirementsDTO), eq(token))
      val returnedValue =
        amendReferralController.amendNeedsAndRequirements(token, referral.id, "non-existing-type", amendNeedsAndRequirementsDTO)
      assertThat(returnedValue.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
    @Test
    fun `amendNeedsAndRequirements updates details in referral for the correct type  for amendReferralService updateAmendAccessibilityNeedsDTO`() {
      val amendNeedsAndRequirementsDTO = AmendNeedsAndRequirementsDTO(true, "9-12AM", "A reason for change")
      doNothing().whenever(amendReferralService)
        .updateAmendAccessibilityNeedsDTO(eq(referral.id), eq(amendNeedsAndRequirementsDTO), eq(token))
      val returnedValue =
        amendReferralController.amendNeedsAndRequirements(token, referral.id, "accessibility_needs", amendNeedsAndRequirementsDTO)
      assertThat(returnedValue.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `amendNeedsAndRequirements returns bad request for incorrect correct type  for amendReferralService updateAmendAccessibilityNeedsDTO`() {
      val amendNeedsAndRequirementsDTO = AmendNeedsAndRequirementsDTO(true, "9-12AM", "A reason for change")
      doNothing().whenever(amendReferralService)
        .updateAmendAccessibilityNeedsDTO(eq(referral.id), eq(amendNeedsAndRequirementsDTO), eq(token))
      val returnedValue =
        amendReferralController.amendNeedsAndRequirements(token, referral.id, "non-existing-type", amendNeedsAndRequirementsDTO)
      assertThat(returnedValue.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }

  @Nested
  inner class Changelog {
    private val referral = referralFactory.createSent()
    private val user = authUserFactory.create()
    private val token = tokenFactory.create(userID = user.id, userName = user.userName, authSource = user.authSource)
    private val userMapper = mock<UserMapper>()

    @Test
    fun `getChangelog returns multiple changelog entries`() {
      whenever(
        amendReferralService.getSentReferralForAuthenticatedUser(
          eq(referral.id),
          eq(token)
        )
      ).thenReturn(referral)

      val changeLogValuesList = mutableListOf(
        Changelog(
          referral.id,
          UUID.randomUUID(),
          AmendTopic.COMPLEXITY_LEVEL,
          ReferralAmendmentDetails(mutableListOf("a value")),
          ReferralAmendmentDetails(mutableListOf("another value")),
          "A reason",
          OffsetDateTime.now(),
          user
        ),
        Changelog(
          referral.id,
          UUID.randomUUID(),
          AmendTopic.COMPLEXITY_LEVEL,
          ReferralAmendmentDetails(mutableListOf("a value 2")),
          ReferralAmendmentDetails(mutableListOf("another value 2")),
          "Another reason",
          OffsetDateTime.now(),
          user
        )
      )

      whenever(amendReferralService.getListOfChangeLogEntries(eq(referral))).thenReturn(changeLogValuesList)
      whenever(userMapper.fromToken(any())).thenReturn(user)

      val userdetail = UserDetail("firstname", "email", "lastname")

      whenever(hmppsAuthService.getUserDetail(eq(user))).thenReturn(userdetail)

      val returnedChangeLogObject = amendReferralController.getChangelog(referral.id, token)

      assertThat(returnedChangeLogObject?.size).isEqualTo(2)
      assertThat(returnedChangeLogObject?.get(0)?.changelogId).isEqualTo(changeLogValuesList.get(0).id)
      assertThat(returnedChangeLogObject?.get(0)?.referralId).isEqualTo(changeLogValuesList.get(0).referralId)
    }

    @Test
    fun `getChangelog returns no changelog entries`() {
      whenever(
        amendReferralService.getSentReferralForAuthenticatedUser(
          eq(referral.id),
          eq(token)
        )
      ).thenReturn(referral)

      val changeLogValuesList = listOf<uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog>()
      whenever(amendReferralService.getListOfChangeLogEntries(eq(referral))).thenReturn(changeLogValuesList)

      val returnedChangeLogObject = amendReferralController.getChangelog(referral.id, token)

      assertThat(returnedChangeLogObject?.size).isEqualTo(0)
    }
  }
}
