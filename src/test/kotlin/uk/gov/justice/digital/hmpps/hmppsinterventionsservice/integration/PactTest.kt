package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.InterventionRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@Provider("Interventions Service")
@PactBroker
@Tag("pact")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test", "local")
class PactTest {
  @Autowired private lateinit var referralRepository: ReferralRepository
  @Autowired private lateinit var referralService: ReferralService

  private val accommodationInterventionID = UUID.fromString("98a42c61-c30f-4beb-8062-04033c376e2d")
  private val deliusUser = AuthUser("8751622134", "delius")

  @BeforeEach
  fun setup() {
    // start with a clean slate for referrals on each test.
    // auth users, contracts, service providers, service categories,
    // regions, interventions, etc. are loaded from the test data migrations
    referralRepository.deleteAll()
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext) {
    context.verifyInteraction()
  }

  @State("an intervention has been selected and a draft referral can be created")
  fun `no referrals, using intervention id 98a42c61 from seed data`() {}

  @State("There is an existing draft referral with ID of ac386c25-52c8-41fa-9213-fcf42e24b0b5")
  fun `create referral with the required id`() {
    referralService.createDraftReferral(
      user = deliusUser,
      crn = "X123456",
      interventionId = accommodationInterventionID,
      UUID.fromString("ac386c25-52c8-41fa-9213-fcf42e24b0b5"),
    )
  }

  @State("a draft referral with ID dfb64747-f658-40e0-a827-87b4b0bdcfed exists")
  fun `create a new draft referral with no additional fields set`() {
    val referral = referralService.createDraftReferral(
      user = deliusUser,
      crn = "X862134",
      interventionId = accommodationInterventionID,
      UUID.fromString("dfb64747-f658-40e0-a827-87b4b0bdcfed"),
    )
    referral.createdAt = OffsetDateTime.parse("2020-12-07T20:45:21.986389+00:00")
    referralRepository.save(referral)
  }

//  @State("a service category with ID 428ee70f-3001-4399-95a6-ad25eaaede16 exists")
//  fun `use service category 428ee70f from the seed`() {
//  }
//
//  @State("There is an existing draft referral with ID of d496e4a7-7cc1-44ea-ba67-c295084f1962, and it has had a service category selected")
//  fun `use referral d496e4a7 from the seed`() {}
//
//  @State("There is an existing draft referral with ID of d496e4a7-7cc1-44ea-ba67-c295084f1962, and it has had a service provider selected")
//  fun `use referral d496e4a7 from the seed, since it also has a service provider`() {}
//
//  @State("There is an existing draft referral with ID of 037cc90b-beaa-4a32-9ab7-7f79136e1d27, and it has had desired outcomes selected")
//  fun `use referral 037cc90b from the seed, since it also has desired outcomes`() {}
//
//  @State("There is an existing draft referral with ID of 1219a064-709b-4b6c-a11e-10b8cb3966f6, and it has had a service user selected")
//  fun `use referral 1219a064 from the seed`() {}
//
//  @State("a single referral for user with ID 8751622134 exists")
//  fun `user referral dfb64747 from the seed`() {}
//
//  @State("a referral does not exist for user with ID 123344556")
//  fun `no referral required for this state`() {}
//
//  @State("There is an existing sent referral with ID of 81d754aa-d868-4347-9c0f-50690773014e")
//  fun `use referral 81d754aa from the seed`() {}
//
//  @State("a draft referral with ID dfb64747-f658-40e0-a827-87b4b0bdcfed exists and is ready to be sent")
//  fun `not implemented yet - additional fields`() {}
//
//  @State("a service provider with ID 674b47a0-39bf-4514-82ae-61885b9c0cb4 exists")
//  fun `not implement yet - service provider`() {}

//  @State("there are some existing sent referrals")
//  fun `use referrals from seed data`() {}
}
