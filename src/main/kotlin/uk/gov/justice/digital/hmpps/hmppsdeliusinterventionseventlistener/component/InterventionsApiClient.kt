package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class InterventionsApiClient(
  private val interventionsWebClient: WebClient
) : AsyncRestClient(interventionsWebClient)
