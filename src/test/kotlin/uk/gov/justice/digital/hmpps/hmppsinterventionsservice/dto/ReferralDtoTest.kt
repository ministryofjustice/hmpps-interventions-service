package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entities.Referral
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("Referral DTO Tests")
class ReferralDtoTest {

  @Test
  fun `builds valid Referral DTO`() {

    val referralEntity = Referral(
      1,
      UUID.randomUUID(),
      LocalDateTime.of(2020, 8, 1, 8, 0)
    )

    val referralDto = ReferralDto.from(referralEntity)

    assertThat(referralDto.referralId).isEqualTo(referralEntity.referralId)
    assertThat(referralDto.completeByDate).isEqualTo(referralEntity.completeByDate)
  }
}
