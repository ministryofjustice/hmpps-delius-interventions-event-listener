package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.InterventionsApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import java.net.URI
import java.util.UUID
import kotlin.reflect.KClass

@Service
class InterventionsApiService(
  @Value("\${services.interventions-api.baseurl}") private val interventionsBaseURL: String,
  private val interventionsApiClient: InterventionsApiClient
) {
  companion object : KLogging()

  fun <T : Any> get(uri: URI, klass: KClass<T>): Mono<T> {
    return interventionsApiClient.get(uri, klass)
  }

  fun getReferral(referralId: UUID): Mono<SentReferral> {
    return interventionsApiClient.get(UriComponentsBuilder.fromHttpUrl("$interventionsBaseURL/sent-referral/{id}").buildAndExpand(referralId).toUri(), SentReferral::class)
  }

  fun getIntervention(interventionId: UUID): Mono<Intervention> {
    return interventionsApiClient.get(UriComponentsBuilder.fromHttpUrl("$interventionsBaseURL/intervention/{id}").buildAndExpand(interventionId).toUri(), Intervention::class)
  }
}
