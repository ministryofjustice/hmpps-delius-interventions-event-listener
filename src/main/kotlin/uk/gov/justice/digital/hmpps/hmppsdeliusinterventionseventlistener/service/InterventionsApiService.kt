package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.reflect.KClass

@Service
class InterventionsApiService(private val interventionsApiWebClient: WebClient) {
  companion object : KLogging()

  fun <T : Any> get(uri: URI, klass: KClass<T>): Mono<T> {
    return interventionsApiWebClient.get()
      .uri(uri)
      .defaultHeaders()
      .retrieve()
      .bodyToMono(klass.java)
  }

  fun getReferral(referralId: UUID): Mono<SentReferral> {
    return get(UriComponentsBuilder.fromPath("/sent-referral/{id}").buildAndExpand(referralId).toUri(), SentReferral::class)
  }

  fun getIntervention(interventionId: UUID): Mono<Intervention> {
    return get(UriComponentsBuilder.fromPath("/intervention/{id}").buildAndExpand(interventionId).toUri(), Intervention::class)
  }

  private fun WebClient.RequestHeadersSpec<*>.defaultHeaders(): WebClient.RequestHeadersSpec<*> {
    return this
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
  }
}
