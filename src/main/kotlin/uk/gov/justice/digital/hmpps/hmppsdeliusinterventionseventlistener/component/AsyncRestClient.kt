package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

open class AsyncRestClient(val client: WebClient) {
  open fun <T : Any> get(uri: URI, klass: KClass<T>): Mono<T> {
    return client.get()
      .uri(uri)
      .defaultHeaders()
      .retrieve()
      .bodyToMono(klass.java)
  }

  open fun <T : Any> post(uri: URI, body: Any, klass: KClass<T>): Mono<T> {
    return client.post()
      .uri(uri)
      .bodyValue(body)
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
