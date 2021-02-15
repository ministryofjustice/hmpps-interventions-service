package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention

interface InterventionFilterRepository {
  fun findByCriteria(pccRegionIds: List<String>, minimumAge: Int?, maximumAge: Int?): List<Intervention>
}
