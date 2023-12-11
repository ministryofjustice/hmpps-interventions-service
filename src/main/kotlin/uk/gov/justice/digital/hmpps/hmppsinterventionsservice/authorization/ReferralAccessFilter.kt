package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.model.ReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification.ReferralSummarySpecifications

@Component
class ReferralAccessFilter(
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper,
) {

  fun serviceProviderContracts(user: AuthUser): Specification<ReferralSummary> {
    val userScope = serviceProviderAccessScopeMapper.fromUser(user)
    return ReferralSummarySpecifications.withSPAccess(userScope.contracts.map { it.id }.toSet())
  }

  fun probationPractitionerReferrals(referrals: List<DraftReferral>, user: AuthUser): List<DraftReferral> {
    // todo: filter out referrals for limited access offenders (LAOs)
    return referrals
  }
}
