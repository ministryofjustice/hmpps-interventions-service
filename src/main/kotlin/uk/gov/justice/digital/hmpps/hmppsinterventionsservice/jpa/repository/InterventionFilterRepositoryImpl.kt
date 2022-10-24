package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NPSRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegionID
import java.time.LocalDate
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class InterventionFilterRepositoryImpl(
  @Value("\${overrides.show-future-interventions}") private val showFutureInterventions: Boolean,
  private val pccRegionRepository: PCCRegionRepository
) : InterventionFilterRepository {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  override fun findByCriteria(pccRegionIds: List<PCCRegionID>, allowsFemale: Boolean?, allowsMale: Boolean?, minimumAge: Int?, maximumAge: Int?): List<Intervention> {
    val criteriaBuilder = entityManager.criteriaBuilder
    val criteriaQuery = criteriaBuilder.createQuery(Intervention::class.java)
    val root = criteriaQuery.from(Intervention::class.java)

    val regionPredicate: Predicate? = getRegionPredicate(criteriaBuilder, root, pccRegionIds)
    val allowsFemalePredicate: Predicate? = getAllowsFemalePredicate(criteriaBuilder, root, allowsFemale)
    val allowsMalePredicate: Predicate? = getAllowsMalePredicate(criteriaBuilder, root, allowsMale)
    val minimumAgePredicate: Predicate? = getMinimumAgePredicate(criteriaBuilder, root, minimumAge)
    val maximumAgePredicate: Predicate? = getMaximumAgePredicate(criteriaBuilder, root, maximumAge)
    val startDatePredicate: Predicate? = filterFutureReferrals(criteriaBuilder, root, LocalDate.now())

    val predicates = listOfNotNull(regionPredicate, allowsFemalePredicate, allowsMalePredicate, minimumAgePredicate, maximumAgePredicate, startDatePredicate)
    val finalPredicate: Predicate = criteriaBuilder.and(*predicates.toTypedArray())

    criteriaQuery.where(finalPredicate)
    return entityManager.createQuery(criteriaQuery).resultList
  }

  private fun getRegionPredicate(criteriaBuilder: CriteriaBuilder, root: Root<Intervention>, pccRegionIds: List<PCCRegionID>): Predicate? {

    if (pccRegionIds.isNullOrEmpty()) {
      return null
    }

    val pccRegionPredicate = getPccRegionPredicate(root, pccRegionIds)
    val npsRegionPredicate = getNpsRegionPredicate(root, pccRegionIds)
    return criteriaBuilder.or(pccRegionPredicate, npsRegionPredicate)
  }

  private fun getPccRegionPredicate(root: Root<Intervention>, pccRegionIds: List<PCCRegionID>): Predicate {

    val expression = root.get<DynamicFrameworkContract>("dynamicFrameworkContract").get<PCCRegion>("pccRegion").get<String>("id")
    return expression.`in`(pccRegionIds)
  }

  private fun getNpsRegionPredicate(root: Root<Intervention>, pccRegionIds: List<PCCRegionID>): Predicate {

    val expression = root.get<DynamicFrameworkContract>("dynamicFrameworkContract").get<NPSRegion>("npsRegion").get<Char>("id")
    val npsRegions = pccRegionRepository.findAllByIdIn(pccRegionIds).map { it.npsRegion.id }.distinct()
    return expression.`in`(npsRegions)
  }

  private fun getAllowsFemalePredicate(criteriaBuilder: CriteriaBuilder, root: Root<Intervention>, allowsFemale: Boolean?): Predicate? {
    if (allowsFemale == null) {
      return null
    }
    return criteriaBuilder.equal(root.get<DynamicFrameworkContract>("dynamicFrameworkContract")?.get<Boolean>("allowsFemale"), allowsFemale)
  }

  private fun getAllowsMalePredicate(criteriaBuilder: CriteriaBuilder, root: Root<Intervention>, allowsMale: Boolean?): Predicate? {
    if (allowsMale == null) {
      return null
    }
    return criteriaBuilder.equal(root.get<DynamicFrameworkContract>("dynamicFrameworkContract")?.get<Boolean>("allowsMale"), allowsMale)
  }

  private fun getMinimumAgePredicate(criteriaBuilder: CriteriaBuilder, root: Root<Intervention>, minimumAge: Int?): Predicate? {

    return minimumAge?.let {
      val expression = root.get<DynamicFrameworkContract>("dynamicFrameworkContract").get<Int>("minimumAge")
      criteriaBuilder.equal(expression, minimumAge)
    }
  }

  private fun getMaximumAgePredicate(criteriaBuilder: CriteriaBuilder, root: Root<Intervention>, maximumAge: Int?): Predicate? {

    return maximumAge?.let {
      val expression = root.get<DynamicFrameworkContract>("dynamicFrameworkContract").get<Int>("maximumAge")
      criteriaBuilder.equal(expression, maximumAge)
    }
  }

  private fun filterFutureReferrals(criteriaBuilder: CriteriaBuilder, root: Root<Intervention>, startDate: LocalDate?): Predicate? {
    if (showFutureInterventions) {
      return null
    }
    return startDate?.let {
      val expression = root.get<DynamicFrameworkContract>("dynamicFrameworkContract").get<LocalDate>("referralStartDate")
      criteriaBuilder.lessThanOrEqualTo(expression, startDate)
    }
  }
}
