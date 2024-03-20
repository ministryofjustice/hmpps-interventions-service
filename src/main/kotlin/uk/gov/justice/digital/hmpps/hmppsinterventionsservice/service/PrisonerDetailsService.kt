package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional
class PrisonerDetailsService(
  private val communityAPIOffenderService: CommunityAPIOffenderService,
  private val prisonerOffenderSearchService: PrisonerOffenderSearchService,
) {
  companion object : KLogging()

  fun details(serviceUserCRN: String): Prisoner? {
    val offenderNomsId = communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN)
      .onErrorResume {
        Mono.empty()
      }
      .block()?.primaryIdentifiers?.nomsNumber ?: return null
    logger.info("The nomis Id returned for crn = $serviceUserCRN is $offenderNomsId")

    val prisonDetails = prisonerOffenderSearchService.getPrisonerById(offenderNomsId)
      .onErrorResume {
        Mono.empty()
      }
      .block()
    logger.info("The prison Id returned for nomis Id = $offenderNomsId is ${prisonDetails?.prisonId}")
    return prisonDetails
  }
}
