package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser

data class AuthUserDTO(
  val username: String,
  val authSource: String,
  val userId: String,
) {
  companion object {
    fun from(user: AuthUser): AuthUserDTO {
      return AuthUserDTO(
        username = user.userName,
        authSource = user.authSource,
        userId = user.id,
      )
    }
  }
}
