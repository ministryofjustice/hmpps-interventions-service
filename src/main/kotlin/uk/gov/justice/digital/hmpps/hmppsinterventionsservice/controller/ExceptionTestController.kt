package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('INTERVENTIONS_SERVICE')")
class ExceptionTestController {

  @GetMapping("/exception")
  fun getException() {
    throw Exception("testing - remove later")
  }
}
