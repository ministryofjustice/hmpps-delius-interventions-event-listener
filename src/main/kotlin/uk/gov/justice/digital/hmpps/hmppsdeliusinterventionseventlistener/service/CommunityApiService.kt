package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.SentReferral
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.NotNull

@Service
class CommunityApiService(
  private val communityApiWebClient: WebClient,
  @Value("\${community-api.notification-request-url}") private val communityApiNotificationUrl: String,
  @Value("\${community-api.integration-context}") private val integrationContext: String,
) {
  companion object : KLogging()

  fun getInfo() {
    communityApiWebClient.get()
      .uri("/info")
      .defaultHeaders()
      .retrieve()
      .bodyToMono(String::class.java)
      .subscribe(logger::info)
  }

  fun notifyActionPlanSubmitted(detailUrl: String, actionPlan: ActionPlan, referral: SentReferral, intervention: Intervention) {
    val body = NotificationCreateRequest(
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

    val communityApiUri = UriComponentsBuilder.fromPath(communityApiNotificationUrl)
      .buildAndExpand(referral.serviceUserCRN, referral.relevantSentenceId, integrationContext)
      .toString()

    communityApiWebClient.post()
      .uri(communityApiUri)
      .body(Mono.just(body), NotificationCreateRequest::class.java)
      .retrieve()
      .bodyToMono(Contact::class.java)
      .onErrorMap { error ->
        val errorMessage = when (error) {
          is BadRequest -> error.responseBodyAsString
          else -> error.localizedMessage
        }
        logger.error(
          "Call to community api failed [$errorMessage]",
          error,
          StructuredArguments.kv("req.url", detailUrl),
          StructuredArguments.kv("req.body", body),
          StructuredArguments.kv("res.body", errorMessage),
          StructuredArguments.kv("res.causeMessage", error.message)
        )
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
    return "$eventTypeDescription for $contractTypeName Referral $referenceNumber with Prime Provider $primeProviderName\n" +
      "$url\n(notified via interventions-event-listener)"
  }

  private fun WebClient.RequestHeadersSpec<*>.defaultHeaders(): WebClient.RequestHeadersSpec<*> {
    return this
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
  }
}

data class NotificationCreateRequest(
  val contractType: String,
  val referralStart: OffsetDateTime,
  val referralId: UUID,
  val contactDateTime: OffsetDateTime,
  val notes: String
)

data class Contact(
  @NotNull val contactId: Long
)
