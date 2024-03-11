package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
@Transactional
class PrisonerDetailsService(
  private val communityAPIOffenderService: CommunityAPIOffenderService,
  private val prisonerOffenderSearchService: PrisonerOffenderSearchService,
) {

  fun details(serviceUserCRN: String): Prisoner? {
    val offenderNomsId = communityAPIOffenderService.getOffenderIdentifiers(serviceUserCRN)
      .onErrorResume {
        Mono.empty()
      }
      .block()?.primaryIdentifiers?.nomsNumber ?: return null

    return prisonerOffenderSearchService.getPrisonerById(offenderNomsId)
      .onErrorResume {
        Mono.empty()
      }
      .block()
  }
}
