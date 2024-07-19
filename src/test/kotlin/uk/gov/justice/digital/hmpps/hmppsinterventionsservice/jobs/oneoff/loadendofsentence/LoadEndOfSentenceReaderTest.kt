package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.loadendofsentence

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jobs.oneoff.transferreferrals.LoadEndOfSentenceReader
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.LocalDate

internal class LoadEndOfSentenceReaderTest
@Autowired constructor(
  sessionFactory: SessionFactory,
) : IntegrationTestBase() {
  private val reader = LoadEndOfSentenceReader(sessionFactory)

  fun createPopulatedReferral(): Referral {
    return setupAssistant.createSentReferral(relevantSentenceEndDate = LocalDate.now())
  }

  fun createUnpopulatedReferral(): Referral {
    return setupAssistant.createSentReferral(relevantSentenceEndDate = null)
  }

  @Test
  fun `skips referral that has a valid relevantSentenceEndDate`() {
    val targetReferral = createPopulatedReferral()
    reader.open(ExecutionContext())
    val result = reader.read()
    assertThat(result).isNull()
  }

  @Test
  fun `finds referral that has a null relevantSentenceEndDate`() {
    val targetReferral = createUnpopulatedReferral()
    reader.open(ExecutionContext())
    assertThat(reader.read()?.id).isEqualTo(targetReferral.id)
  }

}
