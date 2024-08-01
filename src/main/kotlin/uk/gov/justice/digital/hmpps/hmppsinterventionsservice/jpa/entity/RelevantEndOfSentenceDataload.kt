package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "relevant_sentence_end_dataload")
class RelevantEndOfSentenceDataload(
  @Id
  @Column(name = "relevant_sentence_id") var relevantSentenceId: Long? = null,
  @Column(name = "relevant_sentence_end_date") var relevantSentenceEndDate: LocalDate? = null,
)
