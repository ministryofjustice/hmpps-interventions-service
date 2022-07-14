package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Changelog
import java.util.UUID

interface ChangelogRepository : JpaRepository<Changelog, UUID>
