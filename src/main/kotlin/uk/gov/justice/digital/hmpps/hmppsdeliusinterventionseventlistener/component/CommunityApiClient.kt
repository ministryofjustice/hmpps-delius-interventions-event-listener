package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.exception.CommunityApiErrorHandler
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

@Component
class CommunityApiClient(
  private val communityApiWebClient: WebClient,
  private val communityApiErrorHandler: CommunityApiErrorHandler,
) {

  fun <T : Any, U : Any> post(uri: String, body: T, returnKlass: KClass<U>): Mono<U> {
    return communityApiWebClient.post()
      .uri(uri)
      .bodyValue(body)
      .defaultHeaders()
      .retrieve()
      .bodyToMono(returnKlass.java)
      .onErrorMap { error ->
        communityApiErrorHandler.handleResponse(error, uri, body)
        throw error
      }
  }

  private fun WebClient.RequestHeadersSpec<*>.defaultHeaders(): WebClient.RequestHeadersSpec<*> {
    return this
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
  }
}
