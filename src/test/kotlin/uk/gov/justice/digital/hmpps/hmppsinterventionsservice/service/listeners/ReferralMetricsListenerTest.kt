package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.listeners

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEvent
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Intervention
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@RepositoryTest
internal class ReferralMetricsListenerTest @Autowired constructor(
  entityManager: TestEntityManager,
  private val referralRepository: ReferralRepository,
) {
  private val registry = SimpleMeterRegistry()
  private val metricsListener = ReferralMetricsListener(registry, referralRepository)
  private val interventionFactory = InterventionFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)

  @AfterEach
  fun clean() {
    registry.clear()
    referralRepository.deleteAll()
  }

  fun createReferral(crn: String, intervention: Intervention, sentAtHour: Int): Referral {
    val newTime = OffsetDateTime.of(2022, 4, 8, sentAtHour, 34, 0, 0, ZoneOffset.UTC)
    return referralFactory.createSent(serviceUserCRN = crn, intervention = intervention, sentAt = newTime)
  }

  fun createEvent(referral: Referral): ReferralEvent {
    return ReferralEvent(
      this,
      ReferralEventType.SENT,
      referral,
      "irrelevant-for-this-test"
    )
  }

  @Test
  fun `re-referring a person to the same intervention measures elapsed time as repeated referral`() {
    val crn = "T123456"
    val intervention = interventionFactory.create()

    listOf(13, 14, 15).forEach { hour ->
      val referral = createReferral(crn, intervention, sentAtHour = hour)
      metricsListener.onApplicationEvent(createEvent(referral))
    }

    registry.find("intervention.referral.repeat_time").timer().let {
      assertThat(it?.count()).isEqualTo(2)
      assertThat(it?.totalTime(TimeUnit.HOURS)).isEqualTo(2.0)
    }
    assertThat(registry.find("intervention.referral.redirect_time").timer()).isNull()
  }

  @Test
  fun `re-referring a person to a different intervention measures elapsed time as redirected referral`() {
    val crn = "T123456"

    listOf(13, 14, 15).forEach { hour ->
      val referral = createReferral(crn, interventionFactory.create(), sentAtHour = hour)
      metricsListener.onApplicationEvent(createEvent(referral))
    }

    registry.find("intervention.referral.redirect_time").timer().let {
      assertThat(it?.count()).isEqualTo(2)
      assertThat(it?.totalTime(TimeUnit.HOURS)).isEqualTo(2.0)
    }
    assertThat(registry.find("intervention.referral.repeat_time").timer()).isNull()
  }

  @Test
  fun `referring a new person does not measure anything`() {
    val crn1 = "T123456"
    val crn2 = "W123456"
    val intervention = interventionFactory.create()

    createReferral(crn1, intervention, sentAtHour = 13)
    val newReferral = createReferral(crn2, intervention, sentAtHour = 14)

    metricsListener.onApplicationEvent(createEvent(newReferral))

    assertThat(registry.find("intervention.referral.repeat_time").timer()).isNull()
    assertThat(registry.find("intervention.referral.redirect_time").timer()).isNull()
  }
}
