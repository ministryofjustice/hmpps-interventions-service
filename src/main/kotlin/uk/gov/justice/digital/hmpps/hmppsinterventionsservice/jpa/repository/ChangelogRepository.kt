package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.AmendTopic
import java.util.UUID

interface ChangelogRepository : JpaRepository<Changelog, UUID> {
  fun findByReferralIdOrderByChangedAtDesc(referralId: UUID): List<Changelog>
  fun deleteByTopic(topic: AmendTopic): Long

  @Query("SELECT c FROM Changelog c WHERE c.topic in :topics")
  fun findByTopicsIn(topics: List<AmendTopic>): List<Changelog>
}
