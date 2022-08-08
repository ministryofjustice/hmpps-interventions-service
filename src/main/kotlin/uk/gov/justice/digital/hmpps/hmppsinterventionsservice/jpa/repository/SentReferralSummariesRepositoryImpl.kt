package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.query.QueryUtils.toOrders
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.support.PageableExecutionUtils
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SentReferralSummary
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

class SentReferralSummariesRepositoryImpl(@PersistenceContext var entityManager: EntityManager) :
  SimpleJpaRepository<SentReferralSummary, UUID>(SentReferralSummary::class.java, entityManager), SentReferralSpecificationExecutor {

  override fun findReferralIds(spec: Specification<SentReferralSummary>, pageable: Pageable): Page<UUID> {

    val criteriaBuilder = this.entityManager.criteriaBuilder

    var criteriaQuery = criteriaBuilder.createQuery(UUID::class.java)
    val root = criteriaQuery.from(SentReferralSummary::class.java)
    criteriaQuery = criteriaQuery.select(root.get("id"))

    val sort: Sort = if (pageable.isPaged) pageable.sort else Sort.unsorted()
    if (sort.isSorted) {
      criteriaQuery.orderBy(toOrders(sort, root, criteriaBuilder))
    }
    val predicate = spec.toPredicate(root, criteriaQuery, criteriaBuilder)
    criteriaQuery.where(predicate)

    val typedQuery = entityManager.createQuery(criteriaQuery)
    if (pageable.isPaged) {
      typedQuery.firstResult = pageable.offset.toInt()
      typedQuery.maxResults = pageable.pageSize
    }
    return PageableExecutionUtils.getPage(typedQuery.resultList, pageable) { this.getCountQuery(spec, SentReferralSummary::class.java).singleResult }
  }
}
