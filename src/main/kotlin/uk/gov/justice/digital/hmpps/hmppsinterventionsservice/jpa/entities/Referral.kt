package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entities

import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity
class Referral(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val referralId: Long? = null,

  val referralUuid: UUID = UUID.randomUUID(),

  var completeByDate: LocalDateTime? = null,

  var createdDate: LocalDateTime? = null,

) : Serializable
