package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.nio.charset.StandardCharsets

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

  private fun WebClient.RequestHeadersSpec<*>.defaultHeaders(): WebClient.RequestHeadersSpec<*> {
    return this
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
  }
}
