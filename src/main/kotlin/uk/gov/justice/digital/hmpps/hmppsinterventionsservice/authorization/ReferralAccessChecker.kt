package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral

@Component
class ReferralAccessChecker(
  private val userTypeChecker: UserTypeChecker,
  private val eligibleProviderMapper: EligibleProviderMapper,
  private val serviceProviderAccessScopeMapper: ServiceProviderAccessScopeMapper,
  private val serviceUserAccessChecker: ServiceUserAccessChecker,
) {
  private val errorMessage = "user does not have access to referral"

  fun forUser(referral: Referral, user: AuthUser, authentication: JwtAuthenticationToken) {
    when {
      userTypeChecker.isProbationPractitionerUser(user) -> forProbationPractitionerUser(referral, user, authentication)
      userTypeChecker.isServiceProviderUser(user) -> forServiceProviderUser(referral, user)
      else -> throw AccessError(errorMessage, listOf("invalid user type"))
    }
  }

  private fun forServiceProviderUser(referral: Referral, user: AuthUser) {
    val userScope = serviceProviderAccessScopeMapper.fromUser(user)
    val eligibleServiceProviders = eligibleProviderMapper.fromReferral(referral)
    val errors = mutableListOf<String>()

    if (!eligibleServiceProviders.contains(userScope.serviceProvider)) {
      errors.add("user's organization is not eligible to access this referral")
    }

    if (!userScope.contracts.contains(referral.intervention.dynamicFrameworkContract)) {
      errors.add("user does not have the required contract group to access this referral")
    }

    if (errors.isNotEmpty()) {
      throw AccessError(errorMessage, errors)
    }
  }

  private fun forProbationPractitionerUser(referral: Referral, user: AuthUser, authentication: JwtAuthenticationToken) {
    // check if the PP is allowed to access this service user's records
    serviceUserAccessChecker.forProbationPractitionerUser(referral.serviceUserCRN, authentication)
  }
}
