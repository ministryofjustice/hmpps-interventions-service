package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.ReferralDto
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService

@RestController
class ReferralController(val referralService: ReferralService) {

  @RequestMapping(path = ["/referrals"], method = [RequestMethod.POST])
  fun createReferral(): ReferralDto {
    return referralService.createReferral()
  }
}
