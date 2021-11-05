package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.hibernate.SessionFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.database.builder.HibernateCursorItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.HMPPSAuthService

@Configuration
class SoftDeleteInvalidUsers(
  private val sessionFactory: SessionFactory,
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val hmppsAuthService: HMPPSAuthService,
) {
  companion object : KLogging() {
    private const val chunkSize = 100
  }

  private val reader = HibernateCursorItemReaderBuilder<AuthUser>()
    .name("softDeleteInvalidUsersReader")
    .sessionFactory(sessionFactory)
    .queryString("select u from AuthUser u")
    .build()

  private val step = stepBuilderFactory.get("softDeleteInvalidUsersStep")
    .chunk<AuthUser, Unit>(chunkSize)
    .reader(reader)
    .processor(
      ItemProcessor { user ->
        if (user.deleted == null) {
          val idIsInvalid = user.authSource == "auth" && !hmppsAuthService.checkAuthUserExists(user.id)

          if (idIsInvalid) {
            logger.info("soft deleting invalid auth user record", StructuredArguments.kv("user_id", user.id))
          }

          user.deleted = idIsInvalid
        }
      }
    )
    .writer { }
    .build()

  @Bean
  fun softDeleteInvalidUsersJob(): Job {
    return jobBuilderFactory["softDeleteInvalidUsersJob"]
      .start(step)
      .build()
  }
}
