package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "referral_service_user_data")
data class ServiceUserData(
  var title: String? = null,
  var firstName: String? = null,
  var lastName: String? = null,
  @Column(name = "dob") var dateOfBirth: LocalDate? = null,
  var gender: String? = null,
  var ethnicity: String? = null,
  var preferredLanguage: String? = null,
  var religionOrBelief: String? = null,

  @Column(name = "disabilities", columnDefinition = "text[]")
  var disabilities: List<String>? = null,

  @OneToOne
  @MapsId
  @JoinColumn(name = "referral_id")
  var draftReferral: DraftReferral? = null,
  @Id
  @Column(name = "referral_id")
  var referralID: UUID? = null,
)
