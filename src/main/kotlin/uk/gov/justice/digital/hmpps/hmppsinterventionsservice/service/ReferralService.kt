package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDto
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entities.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.repository.ReferralRepository
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class ReferralService(private val referralRepository: ReferralRepository) {

  @Transactional
  fun createReferral(): ReferralDto {
    val referral = referralRepository.save(Referral(createdDate = LocalDateTime.now()))
    return ReferralDto.from(referral)
  }
}
