package org.springframework.boot.test.autoconfigure.orm.jpa

import jakarta.persistence.EntityManager

class TestEntityManager(val entityManager: EntityManager) {
  fun <T> persist(entity: T): T {
    entityManager.persist(entity)
    return entity
  }

  fun <T> persistAndFlush(entity: T): T {
    persist(entity)
    flush()
    return entity
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> merge(entity: T): T = entityManager.merge(entity) as T

  fun flush() {
    entityManager.flush()
  }

  fun clear() {
    entityManager.clear()
  }

  fun <T> refresh(entity: T): T {
    entityManager.refresh(entity)
    return entity
  }

  fun <T> find(entityClass: Class<T>, id: Any): T? = entityManager.find(entityClass, id)
}
