package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class MdcLogEnhancerFilter : OncePerRequestFilter() {
  override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
    // this is the place to add more request specific log fields...

    MDC.put("hostname", req.localAddr)
    MDC.put("req_id", req.getHeader("X-Request-Id"))
    try {
      chain.doFilter(req, res)
    } finally {
      // and don't forget to clear them out at the end of each request!
      MDC.remove("hostname")
      MDC.remove("req_id")
    }
  }
}
