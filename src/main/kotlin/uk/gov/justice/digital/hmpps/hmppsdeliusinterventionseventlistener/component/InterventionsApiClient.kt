package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

@Component
class InterventionsApiClient(private val interventionsApiWebClient: WebClient) {

  fun <T : Any> get(uri: URI, klass: KClass<T>): Mono<T> {
    return interventionsApiWebClient.get()
      .uri(uri)
      .defaultHeaders()
      .retrieve()
      .bodyToMono(klass.java)
  }

  private fun WebClient.RequestHeadersSpec<*>.defaultHeaders(): WebClient.RequestHeadersSpec<*> {
    return this
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
  }
}
