package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DraftReferral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProviderSentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.specification.ReferralSpecifications
import java.util.UUID

@Component
class ReferralAccessFilter(
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper,
) {

  fun <T> serviceProviderReferrals(referralSpec: Specification<T>, user: AuthUser): Specification<T> {
    val userScope = serviceProviderAccessScopeMapper.fromUser(user)
    // TODO come back later and fix the bug for throwing error page
    return referralSpec.and(ReferralSpecifications.withSPAccess(userScope.contracts))
  }

  fun serviceProviderReferralSummaries(referrals: List<ServiceProviderSentReferralSummary>, user: AuthUser): List<ServiceProviderSentReferralSummary> {
    val userScope = serviceProviderAccessScopeMapper.fromUser(user)
    return referrals.filter { userScope.contracts.map { contract -> contract.id }.contains(UUID.fromString(it.dynamicFrameWorkContractId)) }
  }

  fun probationPractitionerReferrals(referrals: List<DraftReferral>, user: AuthUser): List<DraftReferral> {
    // todo: filter out referrals for limited access offenders (LAOs)
    return referrals
  }

  fun <T> probationPractitionerReferrals(referralSpec: Specification<T>, user: AuthUser): Specification<T> {
    // todo: filter out referrals for limited access offenders (LAOs)
    return referralSpec
  }
}
