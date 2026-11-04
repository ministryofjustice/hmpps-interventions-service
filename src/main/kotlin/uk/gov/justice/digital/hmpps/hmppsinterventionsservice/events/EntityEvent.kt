package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import jakarta.persistence.EntityManager
import org.hibernate.proxy.HibernateProxy
import org.springframework.context.ApplicationEvent

// An application event that carries JPA entities.
//
// Event listeners run asynchronously, so they must not touch entities (or lazy proxies) belonging to the
// publisher's Hibernate session: a session can't be used from two threads, and Hibernate 7 fails when it is.
// The event multicaster gives each listener a copy of the event from reloadIn, with entities loaded in the
// listener's own session, once the publishing transaction has committed.
interface EntityEvent {
  fun reloadIn(entityManager: EntityManager): ApplicationEvent
}

// loads the current state of an entity in this entity manager, without initialising it if it's a lazy proxy
@Suppress("UNCHECKED_CAST")
fun <T : Any> EntityManager.reload(entity: T): T {
  val entityClass = if (entity is HibernateProxy) entity.hibernateLazyInitializer.persistentClass else entity.javaClass
  val id = entityManagerFactory.persistenceUnitUtil.getIdentifier(entity)
  return find(entityClass, id) as T? ?: throw IllegalStateException("${entityClass.simpleName} $id no longer exists")
}

// reloads any entities in a map of event data, leaving other values as they are
fun EntityManager.reloadEntities(data: Map<String, Any?>): Map<String, Any?> = data.mapValues { (_, value) ->
  if (value != null && isEntity(value)) reload(value) else value
}

private fun EntityManager.isEntity(value: Any): Boolean {
  val valueClass = if (value is HibernateProxy) value.hibernateLazyInitializer.persistentClass else value.javaClass
  return metamodel.entities.any { it.javaType == valueClass }
}
