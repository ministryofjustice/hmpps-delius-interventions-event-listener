package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfiguration(
  @Value("\${services.interventions-api.baseurl}") private val interventionsApiBaseUrl: String,
  @Value("\${services.interventions-api.connect-timeout-seconds}") private val interventionsApiConnectTimeout: Int,
  @Value("\${services.interventions-api.read-timeout-seconds}") private val interventionsApiReadTimeout: Int,
  @Value("\${services.interventions-api.write-timeout-seconds}") private val interventionsApiWriteTimeout: Int,
) {
  companion object {
    const val defaultClientRegistrationId = "interventions-client"
  }

  @Bean
  fun interventionsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    return createAuthorizedWebClient(
      authorizedClientManager,
      interventionsApiBaseUrl,
      interventionsApiConnectTimeout,
      interventionsApiReadTimeout,
      interventionsApiWriteTimeout
    )
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    clientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      clientService
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun createAuthorizedWebClient(
    clientManager: OAuth2AuthorizedClientManager,
    baseUrl: String,
    connectTimeoutInSeconds: Int,
    readTimeoutInSeconds: Int,
    writeTimeoutInSeconds: Int,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientManager)
    oauth2Client.setDefaultClientRegistrationId(defaultClientRegistrationId)

    val httpClient = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutInSeconds * 1000)
      .doOnConnected {
        it
          .addHandlerLast(ReadTimeoutHandler(readTimeoutInSeconds))
          .addHandlerLast(WriteTimeoutHandler(writeTimeoutInSeconds))
      }

    return WebClient.builder()
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .baseUrl(baseUrl)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }
}
