package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.Contact
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.CreateNotificationRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import java.net.URI
import java.time.OffsetDateTime

@Service
class CommunityApiService(
  @Value("\${services.community-api.baseurl}") private val communityApiBaseURL: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUiBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.action-plan}") private val ppActionPlanLocation: String,
  private val communityApiClient: CommunityApiClient,
) {
  companion object : KLogging() {
    const val integrationContext = "commissioned-rehabilitation-services"
    const val communityApiNotificationRequestUrl = "/secure/offenders/crn/{crn}/sentences/{sentenceId}/notifications/context/{contextName}"
  }

  fun notifyActionPlanEvent(
    eventType: EventType,
    occurredAt: OffsetDateTime,
    referral: SentReferral,
    intervention: Intervention,
    descriptionForNotesField: String,
  ): Mono<Contact> {

    val backLinkUrl = buildBackLinkUrl(referral)

    val body = CreateNotificationRequest(
      intervention.contractType.code,
      referral.sentAt,
      referral.id,
      occurredAt,
      buildNotesField(
        intervention.contractType.name,
        intervention.serviceProvider.name,
        referral.referenceNumber,
        backLinkUrl,
        descriptionForNotesField,
      ),
    )

    val communityApiUri = UriComponentsBuilder
      .fromHttpUrl("$communityApiBaseURL$communityApiNotificationRequestUrl")
      .buildAndExpand(referral.serviceUserCRN, referral.relevantSentenceId, integrationContext)
      .toString()

    logger.debug("Community-api request: $communityApiUri, payload: $body")
    return communityApiClient.post(URI.create(communityApiUri), body, Contact::class)
  }

  private fun buildBackLinkUrl(referral: SentReferral): String {
    return UriComponentsBuilder.fromHttpUrl(interventionsUiBaseURL)
      .path(ppActionPlanLocation)
      .buildAndExpand(referral.id)
      .toString()
  }

  private fun buildNotesField(
    contractTypeName: String,
    primeProviderName: String,
    referenceNumber: String,
    backLinkUrl: String,
    eventTypeDescription: String
  ): String {
    return """
      $eventTypeDescription for $contractTypeName Referral $referenceNumber with Prime Provider $primeProviderName
      $backLinkUrl
      (notified via delius-interventions-event-listener)
    """.trimIndent()
  }
}
