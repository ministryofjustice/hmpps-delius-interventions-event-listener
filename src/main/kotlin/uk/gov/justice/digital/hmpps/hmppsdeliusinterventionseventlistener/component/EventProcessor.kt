package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.CommunityApiService
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.InterventionsApiService
import java.net.URI
import java.util.UUID

enum class EventType(val value: String) {
  ACTION_PLAN_SUBMITTED("intervention.action-plan.submitted"),
  DELIVERY_SESSION_FEEDBACK_SUBMITTED("intervention.session-appointment.session-feedback-submitted"),
  SUPPLIER_ASSESSMENT_FEEDBACK_SUBMITTED("intervention.initial-assessment-appointment.session-feedback-submitted");

  companion object {
    private val map = values().associateBy { it.value }
    fun fromType(type: String) = map[type]
  }
}

@Component
class EventProcessor(
  private val interventionsApiService: InterventionsApiService,
  private val communityApiService: CommunityApiService,
  private val featureFlags: FeatureFlags,
) {
  companion object : KLogging()

  fun process(event: InterventionsEvent) {
    val eventType = EventType.fromType(event.eventType) ?: return

    logger.debug("processing event {}", kv("event", event))

    when (eventType) {
      EventType.ACTION_PLAN_SUBMITTED -> processActionPlanSubmittedEvent(event)
      EventType.SUPPLIER_ASSESSMENT_FEEDBACK_SUBMITTED -> processSupplierAssessmentFeedbackSubmittedEvent(event)
      EventType.DELIVERY_SESSION_FEEDBACK_SUBMITTED -> processDeliverySessionFeedbackSubmittedEvent(event)
    }
  }

  fun processActionPlanSubmittedEvent(event: InterventionsEvent) {
    logger.debug("processing notify-action-plan-submitted")
    if (featureFlags.crs["notify-action-plan-submitted"] != true) return

    val actionPlan = interventionsApiService.get(URI(event.detailUrl), ActionPlan::class).block()
    val referral = interventionsApiService.getReferral(actionPlan.referralId).block()
    val intervention = interventionsApiService.getIntervention(referral.interventionId).block()

    logger.debug("notifying nDelius of submitted action plan {} {} {}", kv("actionPlan", actionPlan), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifyActionPlanSubmitted(event.detailUrl, actionPlan, referral, intervention).block()
  }

  private fun processSupplierAssessmentFeedbackSubmittedEvent(event: InterventionsEvent) {
    logger.debug("processing notify-supplier-assessment-feedback-submitted")
    if (featureFlags.crs["notify-supplier-assessment-feedback-submitted"] != true) return

    val supplierAssessment = interventionsApiService.get(URI(event.detailUrl), SupplierAssessment::class).block()
    val deliusAppointmentId = event.get("deliusAppointmentId")
    val referral = interventionsApiService.getReferral(UUID.fromString(event.get("referralId"))).block()
    val intervention = interventionsApiService.getIntervention(referral.interventionId).block()

    logger.debug("notifying nDelius of submitted supplier assessment feedback {} {} {}", kv("supplierAssessment", supplierAssessment), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifySupplierAssessmentFeedbackSubmitted(event.detailUrl, supplierAssessment, deliusAppointmentId, referral, intervention).block()
  }

  private fun processDeliverySessionFeedbackSubmittedEvent(event: InterventionsEvent) {
    logger.debug("processing notify-delivery-session-feedback-submitted")
    if (featureFlags.crs["notify-delivery-session-feedback-submitted"] != true) return

    val deliverySession = interventionsApiService.get(URI(event.detailUrl), DeliverySession::class).block()
    val deliusAppointmentId = event.get("deliusAppointmentId")
    val referral = interventionsApiService.getReferral(UUID.fromString(event.get("referralId"))).block()
    val intervention = interventionsApiService.getIntervention(referral.interventionId).block()

    logger.debug("notifying nDelius of submitted delivery session feedback {} {} {}", kv("deliverySession", deliverySession), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifyDeliverySessionFeedbackSubmitted(event.detailUrl, deliverySession, deliusAppointmentId, referral, intervention).block()
  }
}
