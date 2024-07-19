package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.RelevantEndOfSentenceDataload
import java.time.LocalDate

class RelevantEndOfSentenceFactory(em: TestEntityManager? = null) : EntityFactory(em) {

  fun create(
    sentenceId: Long? = 11111111,
    endDate: LocalDate? = LocalDate.now(),
  ): RelevantEndOfSentenceDataload {
    return save(
      RelevantEndOfSentenceDataload(
        relevantSentenceId = sentenceId,
        relevantSentenceEndDate = endDate,
      ),
    )
  }
}
