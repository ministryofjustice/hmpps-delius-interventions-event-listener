package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.exception.CommunityApiErrorHandler
import java.net.URI
import kotlin.reflect.KClass

@Component
class CommunityApiClient(
  private val interventionsEventWebClient: WebClient,
  private val communityApiErrorHandler: CommunityApiErrorHandler,
) : AsyncRestClient(interventionsEventWebClient) {
  override fun <T : Any> post(uri: URI, body: Any, klass: KClass<T>): Mono<T> {
    return super
      .post(uri, body, klass)
      .onErrorMap { err ->
        communityApiErrorHandler.handleResponse(err, uri.toString(), body)
        throw err
      }
  }
}
