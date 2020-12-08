package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

// i'm assuming that in the justice system people are gender binary,
// and that the likely input for this field is a radio button;
// more research needed.
enum class Gender {
  FEMALE, MALE
}

enum class ComplexityLevel {
  LOW, MEDIUM, HIGH
}

@Entity
data class Referral(
  var caringOrEmploymentResponsibilities: String? = null,
  var disabilities: String? = null,
  var ethnicity: String? = null,
  @Enumerated(EnumType.STRING) var gender: Gender? = null,
  var accessibilityNeeds: String? = null,
  var needs: String? = null,
  var relegionOrBeliefs: String? = null,
  @Enumerated(EnumType.STRING) var complexityLevel: ComplexityLevel? = null,
  var sexualOrientation: String? = null,
  var address: String? = null,
  var crnNumber: String? = null,
  var dob: LocalDate? = null,
  var completionDeadline: LocalDate? = null,
  @CreationTimestamp var created: OffsetDateTime? = null,
  @Id @GeneratedValue var id: UUID? = null,
)
