package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendDesiredOutcomesDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendNeedsAndRequirementsDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ComplexityLevelRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DesiredOutcomeRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ChangeLogFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class AmendReferralServiceUnitTest {
  private val referralRepository: ReferralRepository = mock()
  private val referralService = mock<ReferralService>()
  private val serviceCategoryRepository: ServiceCategoryRepository = mock()
  private val changelogRepository: ChangelogRepository = mock()
  private val complexityLevelRepository: ComplexityLevelRepository = mock()
  private val desiredOutcomeRepository: DesiredOutcomeRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val jwtAuthenticationToken = JwtAuthenticationToken(mock())

  private val userMapper: UserMapper = mock()

  private val referralFactory = ReferralFactory()
  private val serviceCategoryFactory = ServiceCategoryFactory()
  private val tokenFactory = JwtTokenFactory()
  private val changeLogFactory = ChangeLogFactory()
  private val actionPlanFactory = ActionPlanFactory()

  private val amendReferralService = AmendReferralService(
    referralEventPublisher,
    changelogRepository,
    referralRepository,
    serviceCategoryRepository,
    complexityLevelRepository,
    desiredOutcomeRepository,
    userMapper,
    referralService
  )

  @Nested
  inner class UpdateDraftReferralDesiredOutcomes() {
    val authUser = AuthUser("CRN123", "auth", "user")

    @Test
    fun `cant set desired outcomes to an empty list`() {
      val referral = referralFactory.createSent()
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      val e = assertThrows<ServerWebInputException> {
        amendReferralService.updateReferralDesiredOutcomes(
          referral.id,
          AmendDesiredOutcomesDTO(listOf(), "wrong input"),
          jwtAuthenticationToken,
          referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().id,
        )
      }
      assertThat(e.message.equals("desired outcomes cannot be empty"))
    }

    @Test
    fun `cant set desired outcomes when no service categories have been selected`() {
      val referral = referralFactory.createSent()
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      val e = assertThrows<ServerWebInputException> {
        amendReferralService.updateReferralDesiredOutcomes(
          referral.id,
          AmendDesiredOutcomesDTO(listOf(UUID.randomUUID()), "wrong input"),
          jwtAuthenticationToken,
          referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().id,
        )
      }

      assertThat(e.message.equals("desired outcomes cannot be updated: no service categories selected for this referral"))
    }

    @Test
    fun `cant set desired outcomes for an invalid service category`() {
      val serviceCategory1 = serviceCategoryFactory.create()
      val serviceCategory2 = serviceCategoryFactory.create()
      val referral = referralFactory.createSent(selectedServiceCategories = mutableSetOf(serviceCategory1))
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      val e = assertThrows<ServerWebInputException> {
        amendReferralService.updateReferralDesiredOutcomes(
          referral.id,
          AmendDesiredOutcomesDTO(listOf(UUID.randomUUID()), "wrong input"),
          jwtAuthenticationToken,
          serviceCategory2.id
        )
      }
      assertThat(e.message.equals("desired outcomes cannot be updated: specified service category not selected for this referral"))
    }

    @Test
    fun `cant set desired outcome when service category is not found`() {
      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.empty())
      val serviceCategory = serviceCategoryFactory.create()
      val referral = referralFactory.createSent(selectedServiceCategories = mutableSetOf(serviceCategory))
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      val e = assertThrows<ServerWebInputException> {
        amendReferralService.updateReferralDesiredOutcomes(
          referral.id,
          AmendDesiredOutcomesDTO(listOf(UUID.randomUUID()), "wrong input"),
          jwtAuthenticationToken,
          serviceCategory.id
        )
      }

      assertThat(e.message.equals("desired outcomes cannot be updated: specified service category not found"))
    }

    @Test
    fun `cant set desired outcome when the action plan is approved`() {
      val serviceCategoryId = UUID.randomUUID()
      val desiredOutcome = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val serviceCategory = serviceCategoryFactory.create(
        id = serviceCategoryId,
        desiredOutcomes = listOf(desiredOutcome)
      )
      val referral = referralFactory.createSent(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        desiredOutcomes = mutableListOf(desiredOutcome),
        actionPlans = mutableListOf(actionPlanFactory.create(approvedAt = OffsetDateTime.now()))
      )
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

      val e = assertThrows<ServerWebInputException> {
        amendReferralService.updateReferralDesiredOutcomes(
          referral.id,
          AmendDesiredOutcomesDTO(listOf(desiredOutcome.id), "there is a reason"),
          jwtAuthenticationToken,
          serviceCategory.id
        )
      }
      assertThat(e.reason).isEqualTo("desired outcomes cannot be updated: the action plan is already approved")
    }

    @Test
    fun `cant set desired outcomes when its invalid for the service category`() {
      val serviceCategoryId = UUID.randomUUID()
      val desiredOutcome = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val serviceCategory =
        serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = listOf(desiredOutcome))
      val referral = referralFactory.createSent(selectedServiceCategories = mutableSetOf(serviceCategory))

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

      val e = assertThrows<ServerWebInputException> {
        amendReferralService.updateReferralDesiredOutcomes(
          referral.id,
          AmendDesiredOutcomesDTO(listOf(UUID.randomUUID()), "wrong input"),
          jwtAuthenticationToken,
          serviceCategory.id
        )
      }
      assertThat(e.message.equals("desired outcomes cannot be updated: at least one desired outcome is not valid for this service category"))
    }

    @Test
    fun `can update an already selected desired outcome for service category`() {
      val serviceCategoryId = UUID.randomUUID()
      val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val serviceCategory = serviceCategoryFactory.create(
        id = serviceCategoryId,
        desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2)
      )
      val oldReferral = referralFactory.createSent(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        desiredOutcomes = mutableListOf(desiredOutcome1)
      )
      val referral = referralFactory.createSent(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        desiredOutcomes = mutableListOf(desiredOutcome2)
      )
      val expectedChangeLog = changeLogFactory.create(
        oldVal = ReferralAmendmentDetails(listOf(desiredOutcome1.id.toString())),
        newVal = ReferralAmendmentDetails(listOf(desiredOutcome2.id.toString()))
      )

      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(referralRepository.save(any())).thenReturn(referral)
      whenever(changelogRepository.save(any())).thenReturn(expectedChangeLog)

      amendReferralService.updateReferralDesiredOutcomes(
        oldReferral.id,
        AmendDesiredOutcomesDTO(listOf(desiredOutcome2.id), "needs changing"),
        jwtAuthenticationToken,
        serviceCategory.id
      )

      val changelogArgument: ArgumentCaptor<Changelog> = ArgumentCaptor.forClass(Changelog::class.java)
      verify(changelogRepository).save(changelogArgument.capture())
      val expectedChangelog = changelogArgument.value

      assertThat(expectedChangelog?.newVal!!.values.size).isEqualTo(1)
      assertThat(expectedChangelog.newVal.values).containsExactlyInAnyOrder(desiredOutcome2.id.toString())
      assertThat(expectedChangelog.reasonForChange).isEqualTo("needs changing")

      val argument: ArgumentCaptor<Referral> = ArgumentCaptor.forClass(Referral::class.java)
      verify(referralRepository).save(argument.capture())
      val expectedReferral = argument.value

      assertThat(expectedReferral?.selectedDesiredOutcomes!!.size).isEqualTo(1)
      assertThat(expectedReferral.selectedDesiredOutcomes!![0].serviceCategoryId).isEqualTo(serviceCategory.id)
      assertThat(expectedReferral.selectedDesiredOutcomes!![0].desiredOutcomeId).isEqualTo(desiredOutcome2.id)
    }

    @Test
    fun `can update multiple selected desired outcome for a service category`() {
      val serviceCategoryId1 = UUID.randomUUID()
      val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val desiredOutcome3 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val desiredOutcome4 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val serviceCategory =
        serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2, desiredOutcome3, desiredOutcome4))

      val oldReferral = referralFactory.createSent(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        desiredOutcomes = mutableListOf(desiredOutcome1)
      )
      val referral = referralFactory.createSent(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        desiredOutcomes = mutableListOf(desiredOutcome1, desiredOutcome2)
      )

      val expectedChangeLog = changeLogFactory.create(
        oldVal = ReferralAmendmentDetails(listOf(desiredOutcome1.id.toString(), desiredOutcome2.id.toString())),
        newVal = ReferralAmendmentDetails(listOf(desiredOutcome3.id.toString(), desiredOutcome4.id.toString()))
      )

      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(referralRepository.save(any())).thenReturn(referral)
      whenever(changelogRepository.save(any())).thenReturn(expectedChangeLog)

      whenever(changelogRepository.save(any())).thenReturn(expectedChangeLog)

      amendReferralService.updateReferralDesiredOutcomes(
        oldReferral.id,
        AmendDesiredOutcomesDTO(listOf(desiredOutcome3.id, desiredOutcome4.id), "needs changing"),
        jwtAuthenticationToken,
        serviceCategory.id
      )

      val changelogArgument: ArgumentCaptor<Changelog> = ArgumentCaptor.forClass(Changelog::class.java)
      verify(changelogRepository).save(changelogArgument.capture())
      val expectedChangelog = changelogArgument.value

      assertThat(expectedChangelog?.newVal!!.values.size).isEqualTo(2)
      assertThat(expectedChangelog.newVal.values).containsExactlyInAnyOrder(desiredOutcome3.id.toString(), desiredOutcome4.id.toString())
      assertThat(expectedChangelog.reasonForChange).isEqualTo("needs changing")

      val argument: ArgumentCaptor<Referral> = ArgumentCaptor.forClass(Referral::class.java)
      verify(referralRepository).save(argument.capture())
      val expectedReferral = argument.value

      assertThat(expectedReferral?.selectedDesiredOutcomes!!.size).isEqualTo(2)
      assertThat(expectedReferral.selectedDesiredOutcomes!![0].serviceCategoryId).isEqualTo(serviceCategory.id)
      assertThat(expectedReferral.selectedDesiredOutcomes!![0].desiredOutcomeId).isEqualTo(desiredOutcome3.id)
      assertThat(expectedReferral.selectedDesiredOutcomes!![1].serviceCategoryId).isEqualTo(serviceCategory.id)
      assertThat(expectedReferral.selectedDesiredOutcomes!![1].desiredOutcomeId).isEqualTo(desiredOutcome4.id)
    }
  }

  @Nested
  inner class UpdateComplexityLevelId() {

    val authUser = AuthUser("CRN123", "auth", "user")

    @Test
    fun `updateComplexityLevel results in changes being saved`() {

      val complexityLevelId1 = UUID.randomUUID()
      val complexityLevelId2 = UUID.randomUUID()

      val complexityLevel1 = ComplexityLevel(complexityLevelId1, "1", "description")
      val complexityLevel2 = ComplexityLevel(complexityLevelId2, "2", "description")

      val serviceCategory = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel1, complexityLevel2))

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))

      val referral = referralFactory.createSent(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        complexityLevelIds = mutableMapOf(serviceCategory.id to complexityLevel1.id)
      )
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

      val updateToReferral = AmendComplexityLevelDTO(complexityLevelId1, "testing change")

      amendReferralService.updateComplexityLevel(referral.id, updateToReferral, serviceCategory.id, jwtAuthenticationToken)

      val argumentCaptorReferral = argumentCaptor<Referral>()
      verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

      val referralValues = argumentCaptorReferral.firstValue
      Assertions.assertEquals(complexityLevelId1, referralValues.complexityLevelIds?.get(serviceCategory.id))

      val argumentCaptorChangelog = argumentCaptor<Changelog>()
      verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

      val changeLogValues = argumentCaptorChangelog.firstValue
      assertThat(changeLogValues.id).isNotNull
      Assertions.assertEquals(authUser.id, changeLogValues.changedBy.id)
    }

    @Test
    fun `no sent referral found error returned when appropriate `() {
      val referralId = UUID.randomUUID()
      val token = tokenFactory.create()

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(null)

      val e = assertThrows<ResponseStatusException> {
        amendReferralService.getSentReferralForAuthenticatedUser(referralId, token)
      }
      assertThat(e.status).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(e.message).contains("sent referral not found [id=$referralId]")
    }

    @Test
    fun `cant amend complexity level when the action plan is approved`() {

      val complexityLevelId1 = UUID.randomUUID()

      val complexityLevel1 = ComplexityLevel(complexityLevelId1, "1", "description")

      val serviceCategory = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel1))

      val referral = referralFactory.createSent(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        complexityLevelIds = mutableMapOf(serviceCategory.id to complexityLevel1.id),
        actionPlans = mutableListOf(actionPlanFactory.create(approvedAt = OffsetDateTime.now()))
      )
      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)

      val e = assertThrows<ServerWebInputException> {
        amendReferralService.updateComplexityLevel(
          referral.id,
          AmendComplexityLevelDTO(complexityLevelId1, "testing change"),
          serviceCategory.id,
          jwtAuthenticationToken
        )
      }
      assertThat(e.reason).isEqualTo("complexity level cannot be updated: the action plan is already approved")
    }
  }

  @Nested
  inner class UpdateNeedsAndRequirementsCaringOrEmploymentResponsibilities() {

    val authUser = AuthUser("CRN123", "auth", "user")

    @Test
    fun `has additional responsibilities is being amended to true and unavailability time is updated`() {

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

      val referral = referralFactory.createSent(
        hasAdditionalResponsibilities = false
      )
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)

      val updateToReferral = AmendNeedsAndRequirementsDTO(hasAdditionalResponsibilities = true, whenUnavailable = "9-12AM", reasonForChange = "additional responsibilities changed")

      amendReferralService.amendCaringOrEmploymentResponsibilities(referral.id, updateToReferral, jwtAuthenticationToken)

      val argumentCaptorReferral = argumentCaptor<Referral>()
      verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

      val referralValues = argumentCaptorReferral.firstValue
      assertThat(referralValues.hasAdditionalResponsibilities).isTrue
      assertThat(referralValues.whenUnavailable).isEqualTo("9-12AM")

      val argumentCaptorChangelog = argumentCaptor<Changelog>()
      verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

      val changeLogValues = argumentCaptorChangelog.firstValue
      assertThat(changeLogValues.id).isNotNull
      assertThat(changeLogValues.newVal.values).contains("true", "9-12AM")
      assertThat(changeLogValues.oldVal.values).contains("false")
    }

    @Test
    fun `has additional responsibilities is being amended to false and unavailability time is set to null`() {

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

      val referral = referralFactory.createSent(
        hasAdditionalResponsibilities = true,
        whenUnavailable = "9-12AM"
      )
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)

      val updateToReferral = AmendNeedsAndRequirementsDTO(hasAdditionalResponsibilities = false, reasonForChange = "additional responsibilities changed")

      amendReferralService.amendCaringOrEmploymentResponsibilities(referral.id, updateToReferral, jwtAuthenticationToken)

      val argumentCaptorReferral = argumentCaptor<Referral>()
      verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

      val referralValues = argumentCaptorReferral.firstValue
      assertThat(referralValues.hasAdditionalResponsibilities).isFalse()
      assertThat(referralValues.whenUnavailable).isNull()

      val argumentCaptorChangelog = argumentCaptor<Changelog>()
      verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

      val changeLogValues = argumentCaptorChangelog.firstValue
      assertThat(changeLogValues.id).isNotNull
      assertThat(changeLogValues.newVal.values).contains("false")
      assertThat(changeLogValues.oldVal.values).contains("true", "9-12AM")
    }
  }

  @Test
  fun `has accessibility needs is being amended and updated`() {
    val authUser = AuthUser("CRN123", "auth", "user")
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

    val referral = referralFactory.createSent(
      accessibilityNeeds = "schools"
    )
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
    whenever(referralRepository.save(any())).thenReturn(referral)

    val updateToReferral = AmendNeedsAndRequirementsDTO(accessibilityNeeds = "school", reasonForChange = "accessibility need changed")

    amendReferralService.amendAccessibilityNeeds(referral.id, updateToReferral, jwtAuthenticationToken)

    val argumentCaptorReferral = argumentCaptor<Referral>()
    verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

    val referralValues = argumentCaptorReferral.firstValue
    assertThat(referralValues.accessibilityNeeds).isEqualTo("school")

    val argumentCaptorChangelog = argumentCaptor<Changelog>()
    verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

    val changeLogValues = argumentCaptorChangelog.firstValue
    assertThat(changeLogValues.id).isNotNull
    assertThat(changeLogValues.newVal.values).contains("school")
  }

  @Test
  fun `has identify needs is being amended and updated`() {
    val authUser = AuthUser("CRN123", "auth", "user")
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

    val referral = referralFactory.createSent(
      additionalNeedsInformation = "schools"
    )
    whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
    whenever(referralRepository.save(any())).thenReturn(referral)

    val updateToReferral = AmendNeedsAndRequirementsDTO(additionalNeedsInformation = "school", reasonForChange = "identify need changed")

    amendReferralService.amendIdentifyNeeds(referral.id, updateToReferral, jwtAuthenticationToken)

    val argumentCaptorReferral = argumentCaptor<Referral>()
    verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

    val referralValues = argumentCaptorReferral.firstValue
    assertThat(referralValues.additionalNeedsInformation).isEqualTo("school")

    val argumentCaptorChangelog = argumentCaptor<Changelog>()
    verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

    val changeLogValues = argumentCaptorChangelog.firstValue
    assertThat(changeLogValues.id).isNotNull
    assertThat(changeLogValues.newVal.values).contains("school")
  }

  @Nested
  inner class UpdateNeedsAndRequirementsInterpreterRequired() {

    val authUser = AuthUser("CRN123", "auth", "user")

    @Test
    fun `has interpreter required is being amended to true and interpreter Language is updated`() {

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

      val referral = referralFactory.createSent(
        needsInterpreter = false
      )
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)

      val updateToReferral = AmendNeedsAndRequirementsDTO(needsInterpreter = true, interpreterLanguage = "Yoruba", reasonForChange = "interpreter required changing")

      amendReferralService.amendInterpreterRequired(referral.id, updateToReferral, jwtAuthenticationToken)

      val argumentCaptorReferral = argumentCaptor<Referral>()
      verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

      val referralValues = argumentCaptorReferral.firstValue
      assertThat(referralValues.needsInterpreter).isTrue
      assertThat(referralValues.interpreterLanguage).isEqualTo("Yoruba")

      val argumentCaptorChangelog = argumentCaptor<Changelog>()
      verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

      val changeLogValues = argumentCaptorChangelog.firstValue
      assertThat(changeLogValues.id).isNotNull
      assertThat(changeLogValues.newVal.values).contains("true", "Yoruba")
      assertThat(changeLogValues.oldVal.values).contains("false")
    }

    @Test
    fun `has interpreter required is being amended to false and interpreter Language is set to null`() {

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

      val referral = referralFactory.createSent(
        needsInterpreter = true,
        interpreterLanguage = "Yoruba"
      )
      whenever(referralService.getSentReferralForUser(any(), any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)

      val updateToReferral = AmendNeedsAndRequirementsDTO(needsInterpreter = false, reasonForChange = "interpreter required changing")

      amendReferralService.amendInterpreterRequired(referral.id, updateToReferral, jwtAuthenticationToken)

      val argumentCaptorReferral = argumentCaptor<Referral>()
      verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

      val referralValues = argumentCaptorReferral.firstValue
      assertThat(referralValues.needsInterpreter).isFalse
      assertThat(referralValues.interpreterLanguage).isNull()

      val argumentCaptorChangelog = argumentCaptor<Changelog>()
      verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

      val changeLogValues = argumentCaptorChangelog.firstValue
      assertThat(changeLogValues.id).isNotNull
      assertThat(changeLogValues.newVal.values).contains("false")
      assertThat(changeLogValues.oldVal.values).contains("true", "Yoruba")
    }
  }
}
