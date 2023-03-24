package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import java.time.OffsetDateTime
import java.util.UUID

class InterventionFactory(em: TestEntityManager? = null) : EntityFactory(em) {
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory(em)

  fun create(
    id: UUID? = UUID.randomUUID(),
    createdAt: OffsetDateTime? = null,
    title: String = "Sheffield Housing Services",
    description: String = """Inclusive housing for South Yorkshire

      • Bulleted list
      • With indentation and unicode
    """,
    incomingReferralDistributionEmail: String = "shs-incoming@provider.example.com",
    contract: DynamicFrameworkContract? = null,
  ): Intervention {
    return save(
      Intervention(
        id = id ?: UUID.randomUUID(),
        createdAt = createdAt ?: OffsetDateTime.now(),
        title = title,
        description = description,
        incomingReferralDistributionEmail = incomingReferralDistributionEmail,
        dynamicFrameworkContract = contract ?: dynamicFrameworkContractFactory.create(),
      ),
    )
  }

  fun createWithContractCode(contractReference: String, title: String = "Sheffield Housing Services"): Intervention {
    val contract = dynamicFrameworkContractFactory.create(contractReference = contractReference)
    return create(contract = contract, title = title)
  }
}
