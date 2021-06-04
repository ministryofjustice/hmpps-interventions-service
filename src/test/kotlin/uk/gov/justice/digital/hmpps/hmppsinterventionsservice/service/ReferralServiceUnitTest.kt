package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceProviderAccessScopeMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceUserAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.FieldError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.CancellationReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ActionPlanAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CancellationReasonFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ContractTypeFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.util.Optional
import java.util.UUID

class ReferralServiceUnitTest {
  private val authUserRepository: AuthUserRepository = mock()
  private val referralRepository: ReferralRepository = mock()
  private val interventionRepository: InterventionRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val referralConcluder: ReferralConcluder = mock()
  private val referralReferenceGenerator: ReferralReferenceGenerator = mock()
  private val cancellationReasonRepository: CancellationReasonRepository = mock()
  private val actionPlanAppointmentRepository: ActionPlanAppointmentRepository = mock()
  private val serviceCategoryRepository: ServiceCategoryRepository = mock()
  private val referralAccessChecker: ReferralAccessChecker = mock()
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper = mock()
  private val referralAccessFilter: ReferralAccessFilter = mock()
  private val communityAPIReferralService: CommunityAPIReferralService = mock()
  private val userTypeChecker: UserTypeChecker = mock()
  private val serviceUserAccessChecker: ServiceUserAccessChecker = mock()

  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()
  private val cancellationReasonFactory = CancellationReasonFactory()
  private val serviceCategoryFactory = ServiceCategoryFactory()
  private val contractTypeFactory = ContractTypeFactory()
  private val interventionFactory = InterventionFactory()
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory()

  private val referralService = ReferralService(
    referralRepository, authUserRepository, interventionRepository, referralConcluder,
    referralEventPublisher, referralReferenceGenerator, cancellationReasonRepository,
    actionPlanAppointmentRepository, serviceCategoryRepository, referralAccessChecker, userTypeChecker,
    serviceProviderAccessScopeMapper, referralAccessFilter, communityAPIReferralService, serviceUserAccessChecker,
  )

  @Test
  fun `set ended fields on a sent referral`() {
    val referral = referralFactory.createSent()
    val authUser = authUserFactory.create()
    val cancellationReason = cancellationReasonFactory.create()
    val cancellationComments = "comment"

    whenever(authUserRepository.save(authUser)).thenReturn(authUser)
    whenever(referralRepository.save(any())).thenReturn(referralFactory.createEnded(endRequestedComments = cancellationComments))

    val endedReferral = referralService.requestReferralEnd(referral, authUser, cancellationReason, cancellationComments)
    assertThat(endedReferral.endRequestedAt).isNotNull
    assertThat(endedReferral.endRequestedBy).isEqualTo(authUser)
    assertThat(endedReferral.endRequestedReason).isEqualTo(cancellationReason)
    assertThat(endedReferral.endRequestedComments).isEqualTo(cancellationComments)
  }

  @Test
  fun `referral is concluded if eligible on a sent referral`() {
    val referral = referralFactory.createSent()
    val authUser = authUserFactory.create()
    val cancellationReason = cancellationReasonFactory.create()
    val cancellationComments = "comment"

    whenever(authUserRepository.save(authUser)).thenReturn(authUser)
    whenever(referralRepository.save(any())).thenReturn(referralFactory.createEnded(endRequestedComments = cancellationComments))

    referralService.requestReferralEnd(referral, authUser, cancellationReason, cancellationComments)

    verify(referralConcluder).concludeIfEligible(referral)
  }

  @Test
  fun `get all cancellation reasons`() {
    val cancellationReasons = listOf(
      CancellationReason(code = "aaa", description = "reason 1"),
      CancellationReason(code = "bbb", description = "reason 2")
    )
    whenever(cancellationReasonRepository.findAll()).thenReturn(cancellationReasons)
    val result = referralService.getCancellationReasons()
    assertThat(result).isNotNull
  }

  @Nested
  inner class UpdateDraftReferralComplexityLevel() {
    @Test
    fun `cant set complexity level when no service categories have been selected`() {
      val referral = referralFactory.createDraft()
      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralComplexityLevel(
          referral,
          referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().id,
          UUID.randomUUID()
        )
      }

      assertThat(e.message.equals("complexity level cannot be updated: no service categories selected for this referral"))
    }

    @Test
    fun `cant set complexity level for an invalid service category`() {
      val serviceCategory1 = serviceCategoryFactory.create()
      val serviceCategory2 = serviceCategoryFactory.create()
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory1))
      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralComplexityLevel(
          referral,
          serviceCategory2.id,
          UUID.randomUUID()
        )
      }

      assertThat(e.message.equals("complexity level cannot be updated: specified service category not selected for this referral"))
    }

    @Test
    fun `cant set complexity level when service category is not found`() {
      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.empty())
      val serviceCategory = serviceCategoryFactory.create()
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory))
      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralComplexityLevel(
          referral,
          serviceCategory.id,
          UUID.randomUUID()
        )
      }

      assertThat(e.message.equals("complexity level cannot be updated: specified service category not found"))
    }

    @Test
    fun `cant set complexity level when its invalid for the service category`() {
      val complexityLevel = ComplexityLevel(UUID.randomUUID(), "title", "description")
      val serviceCategory = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel))
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory))

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))

      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralComplexityLevel(
          referral,
          serviceCategory.id,
          UUID.randomUUID()
        )
      }

      assertThat(e.message.equals("complexity level cannot be updated: complexity level not valid for this service category"))
    }

    @Test
    fun `can set complexity level for the first time`() {
      val complexityLevel = ComplexityLevel(UUID.randomUUID(), "title", "description")
      val serviceCategory = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel))
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory))

      assertThat(referral.complexityLevelIds).isNull()

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(referralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = referralService.updateDraftReferralComplexityLevel(
        referral,
        serviceCategory.id,
        complexityLevel.id,
      )

      assertThat(updatedReferral.complexityLevelIds!!.size).isEqualTo(1)
    }

    @Test
    fun `can update an already selected complexity level`() {
      val complexityLevel1 = ComplexityLevel(UUID.randomUUID(), "1", "description")
      val complexityLevel2 = ComplexityLevel(UUID.randomUUID(), "2", "description")
      val serviceCategory = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel1, complexityLevel2))
      val referral = referralFactory.createDraft(
        selectedServiceCategories = linkedSetOf(serviceCategory),
        complexityLevelIds = mutableMapOf(serviceCategory.id to complexityLevel1.id)
      )

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(referralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = referralService.updateDraftReferralComplexityLevel(
        referral,
        serviceCategory.id,
        complexityLevel2.id,
      )

      assertThat(updatedReferral.complexityLevelIds!![serviceCategory.id]).isEqualTo(complexityLevel2.id)
    }
  }

  @Nested
  inner class UpdateServiceCategories {

    private val serviceCategoryIds = listOf(
      UUID.fromString("8221a81c-08b2-4262-9c1a-0ab3c82cec8c"),
      UUID.fromString("9556a399-3529-4993-8030-41db2090555e"),
      UUID.fromString("b84f4eb7-4db0-477e-8c59-21027b3262c5"),
      UUID.fromString("c036826e-f077-49a5-8b33-601dca7ad479")
    )

    @Test
    fun `successfully update service categories`() {
      val serviceCategories = linkedSetOf(
        serviceCategoryFactory.create(id = UUID.fromString("8221a81c-08b2-4262-9c1a-0ab3c82cec8c"), name = "aaa"),
        serviceCategoryFactory.create(id = UUID.fromString("9556a399-3529-4993-8030-41db2090555e"), name = "bbb"),
        serviceCategoryFactory.create(id = UUID.fromString("b84f4eb7-4db0-477e-8c59-21027b3262c5"), name = "ccc"),
        serviceCategoryFactory.create(id = UUID.fromString("c036826e-f077-49a5-8b33-601dca7ad479"), name = "ddd")
      )

      val contractType = contractTypeFactory.create(serviceCategories = serviceCategories)
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        )
      )

      val update = DraftReferralDTO(serviceCategoryIds = serviceCategoryIds)
      whenever(referralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)

      referralService.updateDraftReferral(referral, update)

      val argument: ArgumentCaptor<Referral> = ArgumentCaptor.forClass(Referral::class.java)
      verify(referralRepository).save(argument.capture())
      val savedActionPlan = argument.value
      assertThat(savedActionPlan.selectedServiceCategories == serviceCategories)
    }

    @Test
    fun `updating selected service category with new set removes desired outcome for existing service categories`() {

      val serviceCategoryId1 = UUID.randomUUID()
      val serviceCategoryId2 = UUID.randomUUID()
      val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId2)
      val serviceCategory1 = serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = listOf(desiredOutcome1))
      val serviceCategory2 = serviceCategoryFactory.create(id = serviceCategoryId2, desiredOutcomes = listOf(desiredOutcome2))

      val contractType = contractTypeFactory.create(serviceCategories = linkedSetOf(serviceCategory1, serviceCategory2))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = linkedSetOf(serviceCategory1),
        desiredOutcomes = listOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory2))
      whenever(referralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)
      val updatedReferral = referralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId2)))

      assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
      assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId2)
      assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(0)
    }

    @Test
    fun `updating selected service category with same set doesn't remove desired outcome for same service categories`() {

      val serviceCategoryId1 = UUID.randomUUID()
      val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val serviceCategory1 = serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = listOf(desiredOutcome1))

      val contractType = contractTypeFactory.create(serviceCategories = linkedSetOf(serviceCategory1))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = linkedSetOf(serviceCategory1),
        desiredOutcomes = listOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory1))
      whenever(referralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)

      val updatedReferral = referralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId1)))

      verify(referralRepository).save(any())
      assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
      assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId1)
      assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(1)
      assertThat(updatedReferral.selectedDesiredOutcomes!!.elementAt(0).serviceCategoryId).isEqualTo(serviceCategoryId1)
      assertThat(updatedReferral.selectedDesiredOutcomes!!.elementAt(0).desiredOutcomeId).isEqualTo(desiredOutcome1.id)
    }

    @Test
    fun `updating selected service category with new set removes complexity level for existing service categories`() {

      val complexityLevel1 = ComplexityLevel(UUID.randomUUID(), "title", "description")
      val complexityLevel2 = ComplexityLevel(UUID.randomUUID(), "title", "description")
      val serviceCategory1 = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel1))
      val serviceCategory2 = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel2))

      val contractType = contractTypeFactory.create(serviceCategories = linkedSetOf(serviceCategory1, serviceCategory2))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = linkedSetOf(serviceCategory1),
        complexityLevelIds = mapOf(serviceCategory1.id to complexityLevel1.id).toMutableMap()
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory2))
      whenever(referralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)
      val updatedReferral = referralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategory2.id)))

      assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
      assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategory2.id)
      assertThat(updatedReferral.complexityLevelIds).hasSize(0)
    }

    @Test
    fun `updating selected service category with same set doesn't remove complexity level for same service categories`() {

      val complexityLevel1 = ComplexityLevel(UUID.randomUUID(), "title", "description")
      val serviceCategory1 = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel1))

      val contractType = contractTypeFactory.create(serviceCategories = linkedSetOf(serviceCategory1))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = linkedSetOf(serviceCategory1),
        complexityLevelIds = mapOf(serviceCategory1.id to complexityLevel1.id).toMutableMap()
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory1))
      whenever(referralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)
      val updatedReferral = referralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategory1.id)))

      assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
      assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategory1.id)
      assertThat(updatedReferral.complexityLevelIds).hasSize(1)
      assertThat(updatedReferral.complexityLevelIds!![serviceCategory1.id]).isEqualTo(complexityLevel1.id)
    }

    @Test
    fun `fail to update referral with service category not part of contract type`() {
      val serviceCategories = linkedSetOf(
        serviceCategoryFactory.create(id = UUID.fromString("8221a81c-08b2-4262-9c1a-0ab3c82cec8c"), name = "aaa"),
        serviceCategoryFactory.create(id = UUID.fromString("9556a399-3529-4993-8030-41db2090555e"), name = "bbb"),
        serviceCategoryFactory.create(id = UUID.fromString("b84f4eb7-4db0-477e-8c59-21027b3262c5"), name = "ccc")
      )

      val contractType = contractTypeFactory.create(serviceCategories = serviceCategories)
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        )
      )

      val update = DraftReferralDTO(serviceCategoryIds = serviceCategoryIds)
      whenever(referralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(referralRepository.save(any())).thenReturn(referral)

      val exception = Assertions.assertThrows(ValidationError::class.java) {
        referralService.updateDraftReferral(referral, update)
      }
      assertThat(exception.message).isEqualTo("draft referral update invalid")
      assertThat(exception.errors[0]).isEqualTo(FieldError(field = "serviceCategoryIds", error = Code.INVALID_SERVICE_CATEGORY_FOR_CONTRACT))
    }
  }

  @Nested
  inner class UpdateDraftReferralDesiredOutcomes() {

    @Test
    fun `cant set desired outcomes to an empty list`() {
      val referral = referralFactory.createDraft()
      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralDesiredOutcomes(
          referral,
          referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().id,
          listOf()
        )
      }

      assertThat(e.message.equals("desired outcomes cannot be empty"))
    }

    @Test
    fun `cant set desired outcomes when no service categories have been selected`() {
      val referral = referralFactory.createDraft()
      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralDesiredOutcomes(
          referral,
          referral.intervention.dynamicFrameworkContract.contractType.serviceCategories.first().id,
          listOf(UUID.randomUUID())
        )
      }

      assertThat(e.message.equals("desired outcomes cannot be updated: no service categories selected for this referral"))
    }

    @Test
    fun `cant set desired outcomes for an invalid service category`() {
      val serviceCategory1 = serviceCategoryFactory.create()
      val serviceCategory2 = serviceCategoryFactory.create()
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory1))
      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralDesiredOutcomes(
          referral,
          serviceCategory2.id,
          listOf(UUID.randomUUID())
        )
      }

      assertThat(e.message.equals("desired outcomes cannot be updated: specified service category not selected for this referral"))
    }

    @Test
    fun `cant set desired outcome when service category is not found`() {
      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.empty())
      val serviceCategory = serviceCategoryFactory.create()
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory))
      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralDesiredOutcomes(
          referral,
          serviceCategory.id,
          listOf(UUID.randomUUID())
        )
      }

      assertThat(e.message.equals("desired outcomes cannot be updated: specified service category not found"))
    }

    @Test
    fun `cant set desired outcomes when its invalid for the service category`() {
      val serviceCategoryId = UUID.randomUUID()
      val desiredOutcome = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val serviceCategory = serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = listOf(desiredOutcome))
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory))

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))

      val e = assertThrows<ServerWebInputException> {
        referralService.updateDraftReferralDesiredOutcomes(
          referral,
          serviceCategory.id,
          listOf(UUID.randomUUID())
        )
      }

      assertThat(e.message.equals("desired outcomes cannot be updated: at least one desired outcome is not valid for this service category"))
    }

    @Test
    fun `can set desired outcomes for the first time`() {
      val serviceCategoryId = UUID.randomUUID()
      val desiredOutcome = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val serviceCategory = serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = listOf(desiredOutcome))
      val referral = referralFactory.createDraft(selectedServiceCategories = linkedSetOf(serviceCategory))

      assertThat(referral.selectedDesiredOutcomes).isEmpty()

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(referralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = referralService.updateDraftReferralDesiredOutcomes(
        referral,
        serviceCategory.id,
        listOf(desiredOutcome.id),
      )

      assertThat(updatedReferral.selectedDesiredOutcomes!!.size).isEqualTo(1)
      assertThat(updatedReferral.selectedDesiredOutcomes!![0].serviceCategoryId).isEqualTo(serviceCategory.id)
      assertThat(updatedReferral.selectedDesiredOutcomes!![0].desiredOutcomeId).isEqualTo(desiredOutcome.id)
    }

    @Test
    fun `can update an already selected desired outcome for service category`() {
      val serviceCategoryId = UUID.randomUUID()
      val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId)
      val serviceCategory = serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2))
      val referral = referralFactory.createDraft(
        selectedServiceCategories = linkedSetOf(serviceCategory),
        desiredOutcomes = mutableListOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(referralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = referralService.updateDraftReferralDesiredOutcomes(
        referral,
        serviceCategory.id,
        listOf(desiredOutcome2.id),
      )

      assertThat(updatedReferral.selectedDesiredOutcomes!!.size).isEqualTo(1)
      assertThat(updatedReferral.selectedDesiredOutcomes!![0].serviceCategoryId).isEqualTo(serviceCategory.id)
      assertThat(updatedReferral.selectedDesiredOutcomes!![0].desiredOutcomeId).isEqualTo(desiredOutcome2.id)
    }

    @Test
    fun `can update an different selected desired outcome for a different service category`() {
      val serviceCategoryId1 = UUID.randomUUID()
      val serviceCategoryId2 = UUID.randomUUID()
      val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val desiredOutcome2 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId2)
      val serviceCategory1 = serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = listOf(desiredOutcome2))
      val serviceCategory2 = serviceCategoryFactory.create(id = serviceCategoryId2, desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2))
      val referral = referralFactory.createDraft(
        selectedServiceCategories = linkedSetOf(serviceCategory1, serviceCategory2),
        desiredOutcomes = mutableListOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory2))
      whenever(referralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = referralService.updateDraftReferralDesiredOutcomes(
        referral,
        serviceCategory2.id,
        listOf(desiredOutcome2.id),
      )

      assertThat(updatedReferral.selectedDesiredOutcomes!!.size).isEqualTo(2)
      assertThat(updatedReferral.selectedDesiredOutcomes!![0].serviceCategoryId).isEqualTo(serviceCategory1.id)
      assertThat(updatedReferral.selectedDesiredOutcomes!![0].desiredOutcomeId).isEqualTo(desiredOutcome1.id)
      assertThat(updatedReferral.selectedDesiredOutcomes!![1].serviceCategoryId).isEqualTo(serviceCategory2.id)
      assertThat(updatedReferral.selectedDesiredOutcomes!![1].desiredOutcomeId).isEqualTo(desiredOutcome2.id)
    }
  }
}
