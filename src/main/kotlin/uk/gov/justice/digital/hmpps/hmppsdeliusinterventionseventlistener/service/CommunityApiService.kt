package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.AppointmentOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.Contact
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.CreateNotificationRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SessionFeedback
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SupplierAssessment
import java.net.URI

@Service
class CommunityApiService(
  @Value("\${services.community-api.baseurl}") private val communityApiBaseURL: String,
  @Value("\${interventions-ui.baseurl}") private val interventionsUiBaseURL: String,
  private val communityApiClient: CommunityApiClient,
) {
  companion object : KLogging() {
    const val integrationContext = "commissioned-rehabilitation-services"
    const val communityApiNotificationRequestUrl = "/secure/offenders/crn/{crn}/sentences/{sentenceId}/notifications/context/{contextName}"
    const val communityApiAppointmentOutcomeRequestUrl = "/secure/offenders/crn/{crn}/appointments/{appointmentId}/outcome/context/{contextName}"
    const val ppActionPlanLocation = "/probation-practitioner/referrals/{id}/action-plan"
    const val ppSupplierAssessmentFeedbackLocation = "/probation-practitioner/referrals/{id}/supplier-assessment/post-assessment-feedback"
    const val ppDeliverySessionFeedbackLocation = "/probation-practitioner/referrals/{id}/appointment/{sessionNumber}/post-session-feedback"
  }

  fun notifyActionPlanSubmitted(detailUrl: String, actionPlan: ActionPlan, referral: SentReferral, intervention: Intervention): Mono<Contact> {

    val backLinkUrl = UriComponentsBuilder.fromHttpUrl(interventionsUiBaseURL)
      .path(ppActionPlanLocation)
      .buildAndExpand(referral.id)
      .toString()

    val body = CreateNotificationRequest(
      intervention.contractType.code,
      referral.sentAt,
      referral.id,
      actionPlan.submittedAt,
      buildNotesField(
        intervention.contractType.name,
        intervention.serviceProvider.name,
        referral.referenceNumber,
        backLinkUrl,
        "Action Plan Submitted"
      ),
    )

    val communityApiUri = UriComponentsBuilder
      .fromHttpUrl("$communityApiBaseURL$communityApiNotificationRequestUrl")
      .buildAndExpand(referral.serviceUserCRN, referral.relevantSentenceId, integrationContext)
      .toString()

    logger.debug("Community-api request: $communityApiUri, payload: $body")
    return communityApiClient.post(URI.create(communityApiUri), body, Contact::class)
  }

  fun notifySupplierAssessmentFeedbackSubmitted(detailUrl: String, supplierAssessment: SupplierAssessment, deliusAppointmentId: String, referral: SentReferral, intervention: Intervention): Mono<Contact> {

    val backLinkUrl = UriComponentsBuilder.fromHttpUrl(interventionsUiBaseURL)
      .path(ppSupplierAssessmentFeedbackLocation)
      .buildAndExpand(referral.id)
      .toString()

    val sessionFeedback = supplierAssessment.currentAppointment.sessionFeedback
    return notifyAppointmentFeedbackSubmitted(sessionFeedback, intervention, referral, backLinkUrl, deliusAppointmentId)
  }

  fun notifyDeliverySessionFeedbackSubmitted(detailUrl: String, deliverySession: DeliverySession, deliusAppointmentId: String, referral: SentReferral, intervention: Intervention): Mono<Contact> {

    val backLinkUrl = UriComponentsBuilder.fromHttpUrl(interventionsUiBaseURL)
      .path(ppDeliverySessionFeedbackLocation)
      .buildAndExpand(referral.id, deliverySession.sessionNumber)
      .toString()

    val sessionFeedback = deliverySession.sessionFeedback
    return notifyAppointmentFeedbackSubmitted(sessionFeedback, intervention, referral, backLinkUrl, deliusAppointmentId)
  }

  private fun notifyAppointmentFeedbackSubmitted(sessionFeedback: SessionFeedback, intervention: Intervention, referral: SentReferral, backLinkUrl: String, deliusAppointmentId: String): Mono<Contact> {
    val notifyPP = setNotifyPPIfRequired(sessionFeedback)

    val body = AppointmentOutcomeRequest(
      buildNotesField(
        intervention.contractType.name,
        intervention.serviceProvider.name,
        referral.referenceNumber,
        backLinkUrl,
        "Session Feedback Recorded"
      ),
      sessionFeedback.attendance.attended,
      notifyPP
    )

    val communityApiUri = UriComponentsBuilder
      .fromHttpUrl("$communityApiBaseURL$communityApiAppointmentOutcomeRequestUrl")
      .buildAndExpand(referral.serviceUserCRN, deliusAppointmentId, integrationContext)
      .toString()

    logger.debug("Community-api request: $communityApiUri, payload: $body")
    return communityApiClient.post(URI.create(communityApiUri), body, Contact::class)
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

  fun setNotifyPPIfRequired(sessionFeedback: SessionFeedback): Boolean {
    val attendance = sessionFeedback.attendance
    val behaviour = sessionFeedback.behaviour
    return "no".equals(attendance.attended, true) || behaviour.notifyProbationPractitioner ?: false
  }
}
