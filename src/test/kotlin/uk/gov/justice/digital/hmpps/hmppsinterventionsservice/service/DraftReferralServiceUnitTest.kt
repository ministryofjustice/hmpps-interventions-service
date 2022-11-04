package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.server.ServerWebInputException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ReferralAccessFilter
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.ServiceUserAccessChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserTypeChecker
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.Code
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.FieldError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.ValidationError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DraftReferralDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DeliverySessionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.DraftReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ContractTypeFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DraftOasysRiskInformationFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class DraftReferralServiceUnitTest {
  private val authUserRepository: AuthUserRepository = mock()
  private val referralRepository: ReferralRepository = mock()
  private val draftReferralRepository: DraftReferralRepository = mock()
  private val interventionRepository: InterventionRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val referralReferenceGenerator: ReferralReferenceGenerator = mock()
  private val deliverySessionRepository: DeliverySessionRepository = mock()
  private val serviceCategoryRepository: ServiceCategoryRepository = mock()
  private val referralAccessFilter: ReferralAccessFilter = mock()
  private val referralAccessChecker: ReferralAccessChecker = mock()
  private val communityAPIReferralService: CommunityAPIReferralService = mock()
  private val userTypeChecker: UserTypeChecker = mock()
  private val serviceUserAccessChecker: ServiceUserAccessChecker = mock()
  private val assessRisksAndNeedsService: RisksAndNeedsService = mock()
  private val supplierAssessmentService: SupplierAssessmentService = mock()
  private val hmppsAuthService: HMPPSAuthService = mock()
  private val draftOasysRiskInformationService: DraftOasysRiskInformationService = mock()
  private val referralDetailsRepository: ReferralDetailsRepository = mock()

  private val referralFactory = ReferralFactory()
  private val authUserFactory = AuthUserFactory()
  private val serviceCategoryFactory = ServiceCategoryFactory()
  private val contractTypeFactory = ContractTypeFactory()
  private val interventionFactory = InterventionFactory()
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory()
  private val draftOasysRiskInformationFactory = DraftOasysRiskInformationFactory()

  private val draftReferralService = DraftReferralService(
    referralRepository,
    draftReferralRepository,
    referralAccessFilter,
    referralAccessChecker,
    serviceUserAccessChecker,
    authUserRepository,
    interventionRepository,
    referralEventPublisher,
    referralReferenceGenerator,
    deliverySessionRepository,
    serviceCategoryRepository,
    userTypeChecker,
    communityAPIReferralService,
    assessRisksAndNeedsService,
    supplierAssessmentService,
    hmppsAuthService,
    referralDetailsRepository,
    draftOasysRiskInformationService
  )

  @Nested
  inner class UpdateDraftReferralComplexityLevel {
    @Test
    fun `cant set complexity level when no service categories have been selected`() {
      val referral = referralFactory.createDraft()
      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralComplexityLevel(
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
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory1))
      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralComplexityLevel(
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
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory))
      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralComplexityLevel(
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
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory))

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))

      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralComplexityLevel(
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
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory))

      assertThat(referral.complexityLevelIds).isNull()

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(draftReferralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = draftReferralService.updateDraftReferralComplexityLevel(
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
        selectedServiceCategories = mutableSetOf(serviceCategory),
        complexityLevelIds = mutableMapOf(serviceCategory.id to complexityLevel1.id)
      )

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(draftReferralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = draftReferralService.updateDraftReferralComplexityLevel(
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
      val serviceCategories = setOf(
        serviceCategoryFactory.create(id = UUID.fromString("8221a81c-08b2-4262-9c1a-0ab3c82cec8c"), name = "aaa"),
        serviceCategoryFactory.create(id = UUID.fromString("9556a399-3529-4993-8030-41db2090555e"), name = "bbb"),
        serviceCategoryFactory.create(id = UUID.fromString("b84f4eb7-4db0-477e-8c59-21027b3262c5"), name = "ccc"),
        serviceCategoryFactory.create(id = UUID.fromString("c036826e-f077-49a5-8b33-601dca7ad479"), name = "ddd")
      )

      val contractType = contractTypeFactory.create(serviceCategories = serviceCategories.toSet())
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        )
      )

      val update = DraftReferralDTO(serviceCategoryIds = serviceCategoryIds)
      whenever(draftReferralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(draftReferralRepository.save(any())).thenReturn(referral)

      draftReferralService.updateDraftReferral(referral, update)

      val argument: ArgumentCaptor<DraftReferral> = ArgumentCaptor.forClass(DraftReferral::class.java)
      verify(draftReferralRepository).save(argument.capture())
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

      val contractType = contractTypeFactory.create(serviceCategories = setOf(serviceCategory1, serviceCategory2))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = mutableSetOf(serviceCategory1),
        desiredOutcomes = listOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory2))
      whenever(draftReferralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(draftReferralRepository.save(any())).thenReturn(referral)
      val updatedReferral = draftReferralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId2)))

      assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
      assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategoryId2)
      assertThat(updatedReferral.selectedDesiredOutcomes).hasSize(0)
    }

    @Test
    fun `updating selected service category with same set doesn't remove desired outcome for same service categories`() {

      val serviceCategoryId1 = UUID.randomUUID()
      val desiredOutcome1 = DesiredOutcome(UUID.randomUUID(), "title", serviceCategoryId = serviceCategoryId1)
      val serviceCategory1 = serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = listOf(desiredOutcome1))

      val contractType = contractTypeFactory.create(serviceCategories = setOf(serviceCategory1))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = mutableSetOf(serviceCategory1),
        desiredOutcomes = listOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory1))
      whenever(draftReferralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(draftReferralRepository.save(any())).thenReturn(referral)

      val updatedReferral = draftReferralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategoryId1)))

      verify(draftReferralRepository).save(any())
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

      val contractType = contractTypeFactory.create(serviceCategories = setOf(serviceCategory1, serviceCategory2))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = mutableSetOf(serviceCategory1),
        complexityLevelIds = mapOf(serviceCategory1.id to complexityLevel1.id).toMutableMap()
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory2))
      whenever(draftReferralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(draftReferralRepository.save(any())).thenReturn(referral)
      val updatedReferral = draftReferralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategory2.id)))

      assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
      assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategory2.id)
      assertThat(updatedReferral.complexityLevelIds).hasSize(0)
    }

    @Test
    fun `updating selected service category with same set doesn't remove complexity level for same service categories`() {

      val complexityLevel1 = ComplexityLevel(UUID.randomUUID(), "title", "description")
      val serviceCategory1 = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel1))

      val contractType = contractTypeFactory.create(serviceCategories = setOf(serviceCategory1))
      val referral = referralFactory.createDraft(
        intervention = interventionFactory.create(
          contract = dynamicFrameworkContractFactory.create(
            contractType = contractType
          )
        ),
        selectedServiceCategories = mutableSetOf(serviceCategory1),
        complexityLevelIds = mapOf(serviceCategory1.id to complexityLevel1.id).toMutableMap()
      )

      whenever(serviceCategoryRepository.findByIdIn(any())).thenReturn(setOf(serviceCategory1))
      whenever(draftReferralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(draftReferralRepository.save(any())).thenReturn(referral)
      val updatedReferral = draftReferralService.updateDraftReferral(referral, DraftReferralDTO(serviceCategoryIds = listOf(serviceCategory1.id)))

      assertThat(updatedReferral.selectedServiceCategories).hasSize(1)
      assertThat(updatedReferral.selectedServiceCategories!!.elementAt(0).id).isEqualTo(serviceCategory1.id)
      assertThat(updatedReferral.complexityLevelIds).hasSize(1)
      assertThat(updatedReferral.complexityLevelIds!![serviceCategory1.id]).isEqualTo(complexityLevel1.id)
    }

    @Test
    fun `fail to update referral with service category not part of contract type`() {
      val serviceCategories = setOf(
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
      whenever(draftReferralRepository.saveAndFlush(any())).thenReturn(referral)
      whenever(draftReferralRepository.save(any())).thenReturn(referral)

      val exception = Assertions.assertThrows(ValidationError::class.java) {
        draftReferralService.updateDraftReferral(referral, update)
      }
      assertThat(exception.message).isEqualTo("draft referral update invalid")
      assertThat(exception.errors[0]).isEqualTo(FieldError(field = "serviceCategoryIds", error = Code.INVALID_SERVICE_CATEGORY_FOR_CONTRACT))
    }
  }

  @Nested
  inner class UpdateDraftReferralDesiredOutcomes {
    @Test
    fun `cant set desired outcomes to an empty list`() {
      val referral = referralFactory.createDraft()
      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralDesiredOutcomes(
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
        draftReferralService.updateDraftReferralDesiredOutcomes(
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
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory1))
      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralDesiredOutcomes(
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
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory))
      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralDesiredOutcomes(
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
      val serviceCategory =
        serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = listOf(desiredOutcome))
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory))

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))

      val e = assertThrows<ServerWebInputException> {
        draftReferralService.updateDraftReferralDesiredOutcomes(
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
      val serviceCategory =
        serviceCategoryFactory.create(id = serviceCategoryId, desiredOutcomes = listOf(desiredOutcome))
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCategory))

      assertThat(referral.selectedDesiredOutcomes).isEmpty()

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(draftReferralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = draftReferralService.updateDraftReferralDesiredOutcomes(
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
      val serviceCategory = serviceCategoryFactory.create(
        id = serviceCategoryId,
        desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2)
      )
      val referral = referralFactory.createDraft(
        selectedServiceCategories = mutableSetOf(serviceCategory),
        desiredOutcomes = mutableListOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))
      whenever(draftReferralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = draftReferralService.updateDraftReferralDesiredOutcomes(
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
      val serviceCategory1 =
        serviceCategoryFactory.create(id = serviceCategoryId1, desiredOutcomes = listOf(desiredOutcome2))
      val serviceCategory2 = serviceCategoryFactory.create(
        id = serviceCategoryId2,
        desiredOutcomes = listOf(desiredOutcome1, desiredOutcome2)
      )
      val referral = referralFactory.createDraft(
        selectedServiceCategories = mutableSetOf(serviceCategory1, serviceCategory2),
        desiredOutcomes = mutableListOf(desiredOutcome1)
      )

      whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory2))
      whenever(draftReferralRepository.save(referral)).thenReturn(referral)

      val updatedReferral = draftReferralService.updateDraftReferralDesiredOutcomes(
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

  @Test
  fun `cant send draft referral without risk information`() {
    val referral = referralFactory.createDraft()
    val authUser = authUserFactory.create()

    val e = assertThrows<ServerWebInputException> {
      draftReferralService.sendDraftReferral(referral, authUser)
    }

    assertThat(e.message).contains("can't submit a referral without risk information")
  }

  @Test
  fun`draft referral risk information is deleted when referral is sent`() {
    val draftReferral = referralFactory.createDraft(additionalRiskInformation = "something")
    val authUser = authUserFactory.create()

    val referral = referralFactory.createSent(id = draftReferral.id)

    whenever(referralRepository.save(any())).thenReturn(referral)

    val sentReferral = draftReferralService.sendDraftReferral(draftReferral, authUser)
    assertThat(sentReferral.additionalRiskInformation).isNull()
  }

  @Test
  fun `supplementaryRiskId is set when referral is sent`() {
    val draftReferral = referralFactory.createDraft(additionalRiskInformation = "something")
    val authUser = authUserFactory.create()

    val referral = referralFactory.createSent(id = draftReferral.id, supplementaryRiskId = UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))
    whenever(referralRepository.save(any())).thenReturn(referral)
    whenever(assessRisksAndNeedsService.createSupplementaryRisk(eq(referral.id), any(), any(), anyOrNull(), any(), anyOrNull()))
      .thenReturn(UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))

    val sentReferral = draftReferralService.sendDraftReferral(draftReferral, authUser)
    assertThat(sentReferral.supplementaryRiskId).isEqualTo(UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))
  }

  @Nested
  inner class PostingOasysRiskInformation {
    private val draftRisk = draftOasysRiskInformationFactory.create(
      referralId = UUID.randomUUID(),
      riskSummaryWhoIsAtRisk = "riskSummaryWhoIsAtRisk",
      riskSummaryNatureOfRisk = "riskSummaryNatureOfRisk",
      riskSummaryRiskImminence = "riskSummaryRiskImminence",
      riskToSelfSuicide = "riskToSelfSuicide",
      riskToSelfSelfHarm = "riskToSelfSelfHarm",
      riskToSelfHostelSetting = "riskToSelfHostelSetting",
      riskToSelfVulnerability = "riskToSelfVulnerability",
      additionalInformation = "draftOasysAdditionalInformation",
    )

    @Test
    fun `correct additional risk information is set if it exists as draft oasys risk information`() {
      val draftReferral = referralFactory.createDraft(additionalRiskInformation = "something")
      val authUser = authUserFactory.create()

      val referral = referralFactory.createSent(id = draftReferral.id, supplementaryRiskId = UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))
      whenever(referralRepository.save(any())).thenReturn(referral)

      whenever(draftOasysRiskInformationService.getDraftOasysRiskInformation(referral.id)).thenReturn(draftRisk)

      whenever(assessRisksAndNeedsService.createSupplementaryRisk(eq(referral.id), any(), any(), anyOrNull(), eq("draftOasysAdditionalInformation"), any()))
        .thenReturn(UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))

      val sentReferral = draftReferralService.sendDraftReferral(draftReferral, authUser)
      assertThat(sentReferral.supplementaryRiskId).isEqualTo(UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))
    }

    @Test
    fun `no exception thrown on empty additional risk if draft risk information exists`() {
      val draftReferral = referralFactory.createDraft(additionalRiskInformation = null)
      val authUser = authUserFactory.create()

      val referral = referralFactory.createSent(id = draftReferral.id, supplementaryRiskId = UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))
      whenever(referralRepository.save(any())).thenReturn(referral)

      whenever(draftOasysRiskInformationService.getDraftOasysRiskInformation(referral.id)).thenReturn(draftRisk)

      whenever(assessRisksAndNeedsService.createSupplementaryRisk(eq(referral.id), any(), any(), anyOrNull(), any(), any()))
        .thenReturn(UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))

      val sentReferral = draftReferralService.sendDraftReferral(draftReferral, authUser)
      assertThat(sentReferral.supplementaryRiskId).isEqualTo(UUID.fromString("05320402-1e4b-4bdf-8db3-cde991359ba2"))
    }
  }

  @Test
  fun `timestamp is stored when additionalRiskInformation is updated`() {
    val referral = referralFactory.createDraft(additionalRiskInformation = "something")

    whenever(draftReferralRepository.saveAndFlush(any())).thenReturn(referral)
    whenever(draftReferralRepository.save(any())).thenReturn(referral)

    assertThat(referral.additionalRiskInformationUpdatedAt).isNull()
    draftReferralService.updateDraftReferral(referral, DraftReferralDTO(additionalRiskInformation = "risk"))
    assertThat(referral.additionalRiskInformationUpdatedAt).isNotNull()
  }

  @Test
  fun `submitAdditionalRiskInformation uses appropriate referral fields`() {
    val timestamp = OffsetDateTime.now()
    val draftReferral = referralFactory.createDraft(
      additionalRiskInformation = "something",
      additionalRiskInformationUpdatedAt = timestamp
    )
    val authUser = authUserFactory.create()

    val referral = referralFactory.createSent(id = draftReferral.id)

    whenever(referralRepository.save(any())).thenReturn(referral)

    draftReferralService.sendDraftReferral(draftReferral, authUser)
    verify(assessRisksAndNeedsService, times(1))
      .createSupplementaryRisk(referral.id, referral.serviceUserCRN, authUser, timestamp, "something")
  }

  @Test
  fun `referral is sent to assessRisksAndNeeds before communityApi`() {
    val timestamp = OffsetDateTime.now()
    val draftReferral = referralFactory.createDraft(
      additionalRiskInformation = "something",
      additionalRiskInformationUpdatedAt = timestamp
    )
    val sentReferral = referralFactory.createSent(
      id = draftReferral.id,
      additionalRiskInformationUpdatedAt = timestamp
    )
    val authUser = authUserFactory.create()

    whenever(referralRepository.save(any())).thenReturn(sentReferral)

    val sendReferral = draftReferralService.sendDraftReferral(draftReferral, authUser)

    val inOrder = inOrder(assessRisksAndNeedsService, communityAPIReferralService)
    inOrder.verify(assessRisksAndNeedsService, times(1))
      .createSupplementaryRisk(draftReferral.id, draftReferral.serviceUserCRN, authUser, timestamp, "something")
    val eventCaptor = argumentCaptor<Referral>()

    inOrder.verify(communityAPIReferralService, times(1)).send(eventCaptor.capture())
    val capturedReferral = eventCaptor.firstValue
    assertThat(capturedReferral.id).isEqualTo(sendReferral.id)
  }

  @Nested
  inner class UpdateDraftReferralDetails {
    @BeforeEach
    fun setup() {
      whenever(referralRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `new referral details have the same created time as the referral`() {
      val draftReferral = referralFactory.createDraft()

      // there is no existing referral details for this referral id
      whenever(referralDetailsRepository.findLatestByReferralId(draftReferral.id)).thenReturn(null)

      whenever(draftReferralRepository.save(any())).thenReturn(draftReferral)

      draftReferralService.updateDraftReferral(
        draftReferral,
        DraftReferralDTO(
          completionDeadline = LocalDate.of(3022, 3, 28)
        )
      )

      val captor = ArgumentCaptor.forClass(ReferralDetails::class.java)
      verify(referralDetailsRepository).saveAndFlush(captor.capture())

      val referralDetails = captor.value
      assertThat(referralDetails.createdAt).isEqualTo(draftReferral.createdAt)
    }

    @Test
    fun `updates to the referral details for draft referrals do not create new versions`() {
      val draftReferral = referralFactory.createDraft()
      val existingDetails = ReferralDetails(
        UUID.randomUUID(),
        null,
        draftReferral.id,
        draftReferral.createdAt,
        draftReferral.createdBy.id,
        "initial version",
        LocalDate.of(2022, 3, 28),
        null,
        null,
      )

      whenever(draftReferralRepository.save(any())).thenReturn(draftReferral)
      whenever(referralDetailsRepository.findLatestByReferralId(draftReferral.id)).thenReturn(existingDetails)
      draftReferralService.updateDraftReferral(draftReferral, DraftReferralDTO(furtherInformation = "nothing to see here"))

      val captor = ArgumentCaptor.forClass(ReferralDetails::class.java)
      verify(referralDetailsRepository).saveAndFlush(captor.capture())

      val savedDetails = captor.value
      assertThat(savedDetails.id).isEqualTo(existingDetails.id)
      assertThat(savedDetails.createdAt).isEqualTo(existingDetails.createdAt)
      assertThat(savedDetails.reasonForChange).isEqualTo(existingDetails.reasonForChange)
    }

    @Test
    fun `updates to the referral details for draft referrals are saved in the existing version`() {
      val draftReferral = referralFactory.createDraft()
      val existingDetails = ReferralDetails(
        UUID.randomUUID(),
        null,
        draftReferral.id,
        draftReferral.createdAt,
        draftReferral.createdBy.id,
        "initial version",
        LocalDate.of(2022, 3, 28),
        null,
        null,
      )

      whenever(draftReferralRepository.save(any())).thenReturn(draftReferral)

      whenever(referralDetailsRepository.findLatestByReferralId(draftReferral.id)).thenReturn(existingDetails)
      draftReferralService.updateDraftReferral(draftReferral, DraftReferralDTO(maximumEnforceableDays = 22))

      val captor = ArgumentCaptor.forClass(ReferralDetails::class.java)
      verify(referralDetailsRepository).saveAndFlush(captor.capture())

      val savedDetails = captor.value
      assertThat(savedDetails.completionDeadline).isEqualTo(existingDetails.completionDeadline)
      assertThat(savedDetails.maximumEnforceableDays).isEqualTo(22)
    }

    @Test
    fun `all referral details fields are persisted`() {
      val draftReferral = referralFactory.createDraft()

      whenever(referralDetailsRepository.findLatestByReferralId(draftReferral.id)).thenReturn(null)
      whenever(draftReferralRepository.save(any())).thenReturn(draftReferral)

      draftReferralService.updateDraftReferral(
        draftReferral,
        DraftReferralDTO(
          completionDeadline = LocalDate.of(3022, 3, 28),
          furtherInformation = "nothing to see here",
          maximumEnforceableDays = 25,
        )
      )

      val captor = ArgumentCaptor.forClass(ReferralDetails::class.java)
      verify(referralDetailsRepository).saveAndFlush(captor.capture())

      val referralDetails = captor.value
      assertThat(referralDetails.completionDeadline).isEqualTo(LocalDate.of(3022, 3, 28))
      assertThat(referralDetails.furtherInformation).isEqualTo("nothing to see here")
      assertThat(referralDetails.maximumEnforceableDays).isEqualTo(25)
    }
  }
}
