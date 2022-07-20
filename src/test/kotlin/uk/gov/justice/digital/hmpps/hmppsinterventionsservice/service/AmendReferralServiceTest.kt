package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.Arguments
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralAmendmentDetails
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.*
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.*
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.OffsetDateTime
import java.util.*

@RepositoryTest
class AmendReferralServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
  val interventionRepository: InterventionRepository,
  val deliverySessionRepository: DeliverySessionRepository,
  val actionPlanRepository: ActionPlanRepository,
  val endOfServiceReportRepository: EndOfServiceReportRepository,
) {
  private val userFactory = AuthUserFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)
  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val changelogRepository: ChangelogRepository = mock()
  private val userMapper: UserMapper = mock()
  private val referralService: ReferralService = mock()

  private val amendReferralService = AmendReferralService(
    referralEventPublisher,
    changelogRepository,
    referralRepository,
    userMapper,
    referralService
  )

  companion object {
    @JvmStatic
    fun names1234(): List<Arguments> {
      return listOf(
        Arguments.of("bob-smith", "smith", "bob-smith smith"),
        Arguments.of("john", "blue-red", "john blue-red")
      )
    }
  }

  @AfterEach
  fun `clear referrals`() {
    referralRepository.deleteAll()
  }

  @Nested
  @DisplayName("create / find / update / send draft referrals")
  inner class CreateFindUpdateAndSendDraftReferrals {

//    private lateinit var sampleReferral: Referral
//    private lateinit var serviceCategoryId: UUID
//    private lateinit var complexityLevelId: UUID
//    private lateinit var testComplexityLevelIds: MutableMap<UUID, UUID>
//    private lateinit var referral: Referral



    @BeforeEach
    fun beforeEach() {



//      sampleReferral = SampleData.persistReferral(
//        entityManager,
//        SampleData.sampleReferral("X123456", "Harmony Living", complexityLevelIds = testComplexityLevelIds)
//      )
//      sampleIntervention = sampleReferral.intervention
//
//      sampleCohortIntervention = SampleData.persistIntervention(
//        entityManager,
//        SampleData.sampleIntervention(
//          dynamicFrameworkContract = SampleData.sampleContract(
//            primeProvider = sampleReferral.intervention.dynamicFrameworkContract.primeProvider,
//            contractType = SampleData.sampleContractType(
//              name = "Personal Wellbeing",
//              code = "PWB",
//              serviceCategories = mutableSetOf(
//                SampleData.sampleServiceCategory(),
//                SampleData.sampleServiceCategory(name = "Social inclusion")
//              )
//            )
//          )
//        )
//      )
    }

    @Test
    fun `updateComplexityLevel results in changes being saved`() {
      val serviceCategoryId = UUID.randomUUID()
      val complexityLevelId = UUID.randomUUID()
      val testComplexityLevelIds = mutableMapOf(serviceCategoryId to complexityLevelId)

      val referral = referralFactory.createSent(complexityLevelIds = testComplexityLevelIds)

      val update = AmendComplexityLevelDTO(complexityLevelId, serviceCategoryId, "testing change")
      val jwtAuthenticationToken = JwtAuthenticationToken(mock())
      val authUser = AuthUser("CRN123", "auth", "user")
      val oldReferralDetails = ReferralAmendmentDetails(emptyList())
      val newReferralAmendmentDetails = ReferralAmendmentDetails(listOf(complexityLevelId.toString()))
      val changelog = Changelog(
        referral.id,
        any(),
        "COMPLEXITY_LEVEL",
        oldReferralDetails,
        newReferralAmendmentDetails,
        update.reasonForChange,
        OffsetDateTime.now(),
        authUser
      )
//      Key (referral_id, complexity_level_ids_key)=(4d9fb4fc-f4b6-4031-aa9f-342dd505ed51, d53546ce-6ae3-4779-8814-05fde7813123)
//      is not present in table "referral_selected_service_category".

      whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)
      whenever(changelogRepository.save(any())).thenReturn(changelog)

      amendReferralService.updateComplexityLevel(referral, update, jwtAuthenticationToken)

      verify(changelogRepository).save(changelog)
    }

  }
}
