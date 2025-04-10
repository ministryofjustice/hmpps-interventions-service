package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralServiceUserData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceUserData
import java.time.LocalDate

data class ServiceUserDTO(
  var crn: String,
  var title: String? = null,
  var firstName: String? = null,
  var lastName: String? = null,
  var dateOfBirth: LocalDate? = null,
  var gender: String? = null,
  var ethnicity: String? = null,
  var preferredLanguage: String? = null,
  var religionOrBelief: String? = null,
  var disabilities: List<String>? = null,
) {
  companion object {
    fun from(crn: String, serviceUserData: ServiceUserData?): ServiceUserDTO {
      val dto = ServiceUserDTO(crn = crn)
      serviceUserData?.let {
        dto.crn = crn
        dto.title = serviceUserData.title
        dto.firstName = serviceUserData.firstName
        dto.lastName = serviceUserData.lastName
        dto.dateOfBirth = serviceUserData.dateOfBirth
        dto.gender = serviceUserData.gender
        dto.ethnicity = serviceUserData.ethnicity
        dto.preferredLanguage = serviceUserData.preferredLanguage
        dto.religionOrBelief = serviceUserData.religionOrBelief
        dto.disabilities = serviceUserData.disabilities
      }
      return dto
    }

    fun from(crn: String, serviceUserFirstName: String?, serviceUserLastName: String?): ServiceUserDTO = ServiceUserDTO(crn = crn, firstName = serviceUserFirstName, lastName = serviceUserLastName)
    fun from(crn: String, serviceUserData: ReferralServiceUserData?): ServiceUserDTO {
      val dto = ServiceUserDTO(crn = crn)
      serviceUserData?.let {
        dto.crn = crn
        dto.title = serviceUserData.title
        dto.firstName = serviceUserData.firstName
        dto.lastName = serviceUserData.lastName
        dto.dateOfBirth = serviceUserData.dateOfBirth
        dto.gender = serviceUserData.gender
        dto.ethnicity = serviceUserData.ethnicity
        dto.preferredLanguage = serviceUserData.preferredLanguage
        dto.religionOrBelief = serviceUserData.religionOrBelief
        dto.disabilities = serviceUserData.disabilities
      }
      return dto
    }
  }
}
