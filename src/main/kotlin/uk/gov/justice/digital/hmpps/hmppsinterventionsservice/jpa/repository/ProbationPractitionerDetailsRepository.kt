package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ProbationPractitionerDetails
import java.util.UUID

interface ProbationPractitionerDetailsRepository : JpaRepository<ProbationPractitionerDetails, UUID>
