package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.exception.CommunityApiErrorHandler
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CommunityApiService(
  private val communityApiWebClient: WebClient,
  private val communityApiErrorHandler: CommunityApiErrorHandler,
) {
  companion object : KLogging() {
    const val integrationContext = "commissioned-rehabilitation-services"
  }

  fun notifyActionPlanSubmitted(detailUrl: String, actionPlan: ActionPlan, referral: SentReferral, intervention: Intervention) {
    val body = CreateNotificationRequest(
      intervention.contractType.code,
      referral.sentAt,
      referral.id,
      actionPlan.submittedAt,
      buildNotesField(
        intervention.contractType.name,
        intervention.serviceProvider.name,
        referral.referenceNumber,
        detailUrl,
        "Action Plan Submitted"
      ),
    )

    val communityApiUri = UriComponentsBuilder
      .fromPath("/secure/offenders/crn/{crn}/sentences/{sentenceId}/notifications/context/{contextName}")
      .buildAndExpand(referral.serviceUserCRN, referral.relevantSentenceId, integrationContext)
      .toString()

    communityApiWebClient.post()
      .uri(communityApiUri)
      .bodyValue(body)
      .defaultHeaders()
      .retrieve()
      .bodyToMono(Contact::class.java)
      .onErrorMap { error ->
        communityApiErrorHandler.handleResponse(error, communityApiUri, body)
        throw error
      }
      .block()
  }

  private fun buildNotesField(
    contractTypeName: String,
    primeProviderName: String,
    referenceNumber: String,
    url: String,
    eventTypeDescription: String
  ): String {
    return """
      $eventTypeDescription for $contractTypeName Referral $referenceNumber with Prime Provider $primeProviderName
      $url
      (notified via delius-interventions-event-listener)
    """.trimIndent()
  }

  private fun WebClient.RequestHeadersSpec<*>.defaultHeaders(): WebClient.RequestHeadersSpec<*> {
    return this
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
  }
}

private data class CreateNotificationRequest(
  val contractType: String,
  val referralStart: OffsetDateTime,
  val referralId: UUID,
  val contactDateTime: OffsetDateTime,
  val notes: String
)

private data class Contact(
  val contactId: Long
)
