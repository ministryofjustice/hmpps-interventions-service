package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.NPSRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegion
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PCCRegionID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import javax.persistence.TypedQuery

import java.util.ArrayList
import java.security.acl.Owner







class InterventionFilterRepositoryImpl(
  private val pccRegionRepository: PCCRegionRepository
) : InterventionFilterRepository {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  override fun findByCriteria(pccRegionIds: List<PCCRegionID>, allowsFemale: Boolean?, allowsMale: Boolean?, minimumAge: Int?, maximumAge: Int?): List<Intervention> {
    val criteriaBuilder = entityManager.criteriaBuilder
    val criteriaQuery = criteriaBuilder.createQuery(Intervention::class.java)
    val root = criteriaQuery.from(Intervention::class.java)

    val pccRegionPredicate: Predicate? = getPccRegionPredicate(criteriaBuilder, root, pccRegionIds)
    val allowsFemalePredicate: Predicate? = getAllowsFemalePredicate(criteriaBuilder, root, allowsFemale)
    val allowsMalePredicate: Predicate? = getAllowsMalePredicate(criteriaBuilder, root, allowsMale)
    val minimumAgePredicate: Predicate? = getMinimumAgePredicate(criteriaBuilder, root, minimumAge)
    val maximumAgePredicate: Predicate? = getMaximumAgePredicate(criteriaBuilder, root, maximumAge)

    val predicates = listOfNotNull(pccRegionPredicate, allowsFemalePredicate, allowsMalePredicate, minimumAgePredicate, maximumAgePredicate)
    val finalPredicate: Predicate = criteriaBuilder.and(*predicates.toTypedArray())

    criteriaQuery.where(finalPredicate)
    return entityManager.createQuery(criteriaQuery).resultList
  }

  select i.id, i.title, r.pcc_regions_id
    from intervention i
    left join dynamic_framework_contract_pcc_regions r on i.dynamic_framework_contract_id = r.dynamic_framework_contract_id
    where r.pcc_regions_id in ('dorset', 'gloucestershire', 'wiltshire');

  private fun getPccRegionPredicate(criteriaBuilder: CriteriaBuilder, root: Root<Intervention>, pccRegionIds: List<PCCRegionID>): Predicate? {
    return if (pccRegionIds.isEmpty()) null else {

//      val mm = entityManager.metamodel.entity(Intervention::class.java).getAttribute("dynamicFrameworkContract")
//      val mmI = entityManager.metamodel.entity(DynamicFramworkContract::class.java).
//      val pet: cq.from(Pet::class.java)

      val thing = root.join
      root.join<>("dynamic_framework_contract_pcc_regions")

//      val thing = root.join(mm.getSet("pccRegions", PCCRegion::class.java))
//      val contract = root.get<Any>("dynamicFrameworkContract")

//      val thing = mm.getSet("pccRegions", PCCRegion::class.java)

      val predicates = mutableListOf<Predicate>()
//      val pccRegions = root.get<Any>("dynamicFrameworkContract")
//      val pccRegions = root.get<DynamicFrameworkContract>("dynamicFrameworkContract").get<Set<PCCRegion>>("pccRegions")

//      pccRegionIds.forEach {
//        val predicate = criteriaBuilder.isMember(it, pccRegions.get<PCCRegionID>("id"))
//        predicates.add(predicate)
//      }

      criteriaBuilder.or(*predicates.toTypedArray())
    }
  }

//  private fun getNpsRegionPredicate(root: Root<Intervention>, pccRegionIds: List<PCCRegionID>): Predicate {
//
//    val expression = root.get<DynamicFrameworkContract>("dynamicFrameworkContract").get<NPSRegion>("npsRegion").get<Char>("id")
//    val npsRegions = pccRegionRepository.findAllByIdIn(pccRegionIds).map { it.npsRegion.id }.distinct()
//    return expression.`in`(npsRegions)
//  }

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
}
