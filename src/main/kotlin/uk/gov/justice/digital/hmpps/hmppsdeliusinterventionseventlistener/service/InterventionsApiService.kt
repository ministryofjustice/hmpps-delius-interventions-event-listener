package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.SentReferral
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class InterventionsApiService(private val interventionsApiWebClient: WebClient) {
  companion object : KLogging()

  fun getInfo() {
    interventionsApiWebClient.get()
      .uri("/info")
      .defaultHeaders()
      .retrieve()
      .bodyToMono(String::class.java)
      .subscribe(logger::info)
  }

  fun getActionPlan(detailUrl: String): ActionPlan {
    return interventionsApiWebClient.get()
      .uri(UriComponentsBuilder.fromHttpUrl(detailUrl).toUriString())
      .defaultHeaders()
      .retrieve()
      .bodyToMono(ActionPlan::class.java)
      .block()
  }

  fun getReferral(referralId: UUID): SentReferral {
    return interventionsApiWebClient.get()
      .uri(UriComponentsBuilder.fromPath("/sent-referral/{id}").buildAndExpand(referralId).toUriString())
      .defaultHeaders()
      .retrieve()
      .bodyToMono(SentReferral::class.java)
      .block()
  }

  fun getIntervention(interventionId: UUID): Intervention {
    return interventionsApiWebClient.get()
      .uri(UriComponentsBuilder.fromPath("/intervention/{id}").buildAndExpand(interventionId).toUriString())
      .defaultHeaders()
      .retrieve()
      .bodyToMono(Intervention::class.java)
      .block()
  }

  private fun WebClient.RequestHeadersSpec<*>.defaultHeaders(): WebClient.RequestHeadersSpec<*> {
    return this
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
  }
}
