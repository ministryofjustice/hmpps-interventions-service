package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.RelevantEndOfSentenceDataload

interface RelevantEndOfSentenceDataloadRepository : JpaRepository<RelevantEndOfSentenceDataload, Long> {
  @Query("select r from RelevantEndOfSentenceDataload r where r.relevantSentenceId = :relevantSentenceId")
  fun findByRelevantSentenceId(relevantSentenceId: Long): RelevantEndOfSentenceDataload?
}
