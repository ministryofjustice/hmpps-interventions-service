package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import mu.KLogging
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.ResolvableType
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.orm.jpa.SharedEntityManagerCreator
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.EntityEvent

@Configuration
class EventsConfiguration {
  @Bean(name = ["applicationEventMulticaster"])
  fun simpleApplicationEventMulticaster(
    transactionManager: ObjectProvider<PlatformTransactionManager>,
    entityManagerFactory: ObjectProvider<EntityManagerFactory>,
  ): ApplicationEventMulticaster = EntityEventMulticaster(SimpleAsyncTaskExecutor(), transactionManager, entityManagerFactory)
}

// Processes spring events asynchronously.
//
// Events carrying JPA entities (EntityEvent) are only dispatched once the publishing transaction commits, and each
// listener runs in its own transaction with a copy of the event whose entities are loaded in that transaction.
// This stops listeners sharing the publisher's Hibernate session across threads, and means they see committed data.
// The transaction manager and entity manager factory are looked up lazily, as the multicaster is created early on.
class EntityEventMulticaster(
  private val executor: TaskExecutor,
  private val transactionManager: ObjectProvider<PlatformTransactionManager>,
  private val entityManagerFactory: ObjectProvider<EntityManagerFactory>,
) : SimpleApplicationEventMulticaster() {
  companion object : KLogging()

  init {
    setTaskExecutor(executor)
  }

  private val transactionTemplate by lazy { TransactionTemplate(transactionManager.getObject()) }
  private val entityManager: EntityManager by lazy { SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory.getObject()) }

  override fun multicastEvent(event: ApplicationEvent, eventType: ResolvableType?) {
    if (event !is EntityEvent) {
      super.multicastEvent(event, eventType)
      return
    }

    val listeners = getApplicationListeners(event, eventType ?: ResolvableType.forInstance(event))
    val dispatch = {
      listeners.forEach { listener ->
        if (listensForEntityEvents(listener)) {
          executor.execute { invokeWithReloadedEvent(listener, event) }
        } else {
          // generic listeners (e.g. spring's own) don't use the entities, so get the event as published
          executor.execute { invokeListener(listener, event as ApplicationEvent) }
        }
      }
    }

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
          override fun afterCommit() = dispatch()
        },
      )
    } else {
      dispatch()
    }
  }

  private fun listensForEntityEvents(listener: ApplicationListener<*>): Boolean {
    val declaredEventType = ResolvableType.forClass(AopUtils.getTargetClass(listener)).`as`(ApplicationListener::class.java).getGeneric().resolve()
    return declaredEventType != null && EntityEvent::class.java.isAssignableFrom(declaredEventType)
  }

  private fun invokeWithReloadedEvent(listener: ApplicationListener<*>, event: EntityEvent) {
    try {
      transactionTemplate.executeWithoutResult { invokeListener(listener, event.reloadIn(entityManager)) }
    } catch (e: Exception) {
      logger.error("event listener {} failed for {}", listener.javaClass.simpleName, event, e)
    }
  }
}
