package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

@Configuration
@EnableWebSecurity
class SecurityConfiguration : WebSecurityConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http {
      csrf { disable() }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      authorizeRequests {
        authorize("/health/**", permitAll)
        authorize("/info", permitAll)
        authorize(anyRequest, authenticated)
      }
      oauth2ResourceServer {
        jwt {
          jwtAuthenticationConverter = jwtAuthenticationConverter()
        }
      }
    }
  }

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
}
