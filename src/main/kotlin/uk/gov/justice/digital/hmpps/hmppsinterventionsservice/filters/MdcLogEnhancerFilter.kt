package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.filters

import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

@Component
class MdcLogEnhancerFilter : Filter {
  override fun destroy() {}

  @Throws(ServletException::class)
  override fun init(filterConfig: FilterConfig) {
  }

  @Throws(IOException::class, ServletException::class)
  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    val authentication = SecurityContextHolder.getContext().authentication
    MDC.put("userId", authentication.name)
    MDC.put("remoteAddress", servletRequest.remoteAddr)
    filterChain.doFilter(servletRequest, servletResponse)
  }
}
