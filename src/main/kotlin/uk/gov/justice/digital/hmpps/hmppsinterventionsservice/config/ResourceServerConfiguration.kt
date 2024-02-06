package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config

import com.nimbusds.jwt.JWTParser
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.TokenVerifier

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class ResourceServerConfiguration(private val tokenVerifier: TokenVerifier) {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain = http
    .sessionManagement()
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    .and().csrf().disable()
    .authorizeHttpRequests { auth ->
      auth.requestMatchers(
        "/health/**",
        "/prometheus/**",
        "/info",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
      )
        .permitAll()
        .anyRequest().authenticated()
    }.also {
      it.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthenticationConverter())
    }
    .build()

  @Bean
  fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
    // hmpps auth tokens have roles in a custom `authorities` claim.
    // the authorities are already prefixed with `ROLE_`.
    val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
    grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities")
    grantedAuthoritiesConverter.setAuthorityPrefix("")

    val jwtAuthenticationConverter = JwtAuthenticationConverter()
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
    return jwtAuthenticationConverter
  }

  @Bean
  @Profile("!test")
  fun jwtDecoder(properties: OAuth2ResourceServerProperties): JwtDecoder {
    val issuerUri = properties.jwt.issuerUri
    val jwtDecoder: NimbusJwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri) as NimbusJwtDecoder
    val validator = DelegatingOAuth2TokenValidator(JwtValidators.createDefaultWithIssuer(issuerUri), tokenVerifier)
    jwtDecoder.setJwtValidator(validator)
    return jwtDecoder
  }

  @Bean
  @Profile("test")
  fun testJwtDecoder(): JwtDecoder {
    return TestJwtDecoder()
  }
}

internal class TestJwtDecoder : JwtDecoder {
  private val claimSetConverter = MappedJwtClaimSetConverter.withDefaults(emptyMap())

  override fun decode(token: String): Jwt? {
    // extract headers and claims, but do not attempt to verify signature
    val jwt = JWTParser.parse(token)
    val headers = LinkedHashMap<String, Any>(jwt.header.toJSONObject())
    val claims = claimSetConverter.convert(jwt.jwtClaimsSet.claims)

    return Jwt.withTokenValue(token)
      .headers { it.putAll(headers) }
      .claims { it.putAll(claims) }
      .build()
  }
}
