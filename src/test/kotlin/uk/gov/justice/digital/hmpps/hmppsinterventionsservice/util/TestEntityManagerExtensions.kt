package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.reflect.KClass

// Bulk-deletes every row of each entity, in the order given (so list dependent entities first).
// Hibernate 7 refuses to flush while a loaded entity still references one that repository.deleteAll() has removed,
// so clearing tables of related entities has to bypass the persistence context.
fun TestEntityManager.deleteAll(vararg entities: KClass<*>) {
  flush()
  entities.forEach { entity ->
    entityManager.createQuery("delete from ${entity.simpleName}").executeUpdate()
  }
  clear()
}
