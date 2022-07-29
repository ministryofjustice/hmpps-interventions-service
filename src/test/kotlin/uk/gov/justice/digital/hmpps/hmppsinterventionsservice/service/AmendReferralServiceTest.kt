package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import java.util.Optional
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AmendComplexityLevelDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ComplexityLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ChangelogRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ServiceCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory

@RepositoryTest
class AmendReferralServiceTest {
  private val referralRepository: ReferralRepository = mock()
  private val changelogRepository: ChangelogRepository = mock()
  private val referralEventPublisher: ReferralEventPublisher = mock()
  private val serviceCategoryRepository: ServiceCategoryRepository = mock()
  private val userMapper: UserMapper = mock()
  private val referralService: ReferralService = mock()
  private val jwtAuthenticationToken = JwtAuthenticationToken(mock())

  private val referralFactory = ReferralFactory()
  private val serviceCategoryFactory = ServiceCategoryFactory()

  private val amendReferralService = AmendReferralService(
    referralEventPublisher,
    changelogRepository,
    referralRepository,
    userMapper,
    referralService
  )

  @Test
  fun `AmendComplexityLevelDTO results in changes being saved`() {
    val complexityLevelId1 = UUID.randomUUID()
    val complexityLevelId2 = UUID.randomUUID()

    val complexityLevel1 = ComplexityLevel(complexityLevelId1, "1", "description")
    val complexityLevel2 = ComplexityLevel(complexityLevelId2, "2", "description")

    val serviceCategory = serviceCategoryFactory.create(complexityLevels = listOf(complexityLevel1, complexityLevel2))

    val authUser = AuthUser("CRN123", "auth", "user")
    whenever(userMapper.fromToken(jwtAuthenticationToken)).thenReturn(authUser)

    whenever(serviceCategoryRepository.findById(any())).thenReturn(Optional.of(serviceCategory))

    val referral = referralFactory.createSent(
      selectedServiceCategories = mutableSetOf(serviceCategory),
      complexityLevelIds = mutableMapOf(serviceCategory.id to complexityLevel1.id)
    )

    val updateToReferral = AmendComplexityLevelDTO(complexityLevelId1, serviceCategory.id, "testing change")

    amendReferralService.updateComplexityLevel(referral, updateToReferral, jwtAuthenticationToken)

    val argumentCaptorReferral = argumentCaptor<Referral>()
    verify(referralRepository, atLeast(1)).save(argumentCaptorReferral.capture())

    val referralValues = argumentCaptorReferral.firstValue
    assertEquals(complexityLevelId1, referralValues.complexityLevelIds?.get(serviceCategory.id))

    val argumentCaptorChangelog = argumentCaptor<Changelog>()
    verify(changelogRepository, atLeast(1)).save(argumentCaptorChangelog.capture())

    val changeLogValues = argumentCaptorChangelog.firstValue
    Assertions.assertThat(changeLogValues.id).isNotNull()
    assertEquals(authUser.id, changeLogValues.changedBy.id)
  }
}
