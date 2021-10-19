package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.FeatureFlags
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import java.net.URI

enum class EventType(val eventTypeValue: String) {
  ACTION_PLAN_SUBMITTED("intervention.action-plan.submitted"),
  ACTION_PLAN_APPROVED("intervention.action-plan.approved");

  companion object {
    private val map = values().associateBy { it.eventTypeValue }
    fun fromType(type: String) = map[type]
  }
}

@Service
class EventProcessor(
  private val interventionsApiService: InterventionsApiService,
  private val communityApiService: CommunityApiService,
  private val featureFlags: FeatureFlags,
) {
  companion object : KLogging()

  fun process(event: InterventionsEvent) {
    val eventType = EventType.fromType(event.eventType) ?: return

    when (eventType) {
      EventType.ACTION_PLAN_SUBMITTED -> processActionPlanSubmittedEvent(eventType, event)
      EventType.ACTION_PLAN_APPROVED -> processActionPlanApprovedEvent(eventType, event)
    }
  }

  fun processActionPlanSubmittedEvent(eventType: EventType, event: InterventionsEvent) {
    logger.debug("checking flag for notify-action-plan-submitted")
    if (featureFlags.crs["notify-action-plan-submitted"] != true) return

    logger.debug("processing notify-action-plan-submitted")
    val (actionPlan, referral, intervention) = getActionPlanReferralAndIntervention(event)

    val descriptionForNotesField = "Action Plan Submitted"
    logger.debug("notifying nDelius of $descriptionForNotesField {} {} {}", kv("actionPlan", actionPlan), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifyActionPlanEvent(eventType, event.occurredAt, referral, intervention, descriptionForNotesField).block()
  }

  fun processActionPlanApprovedEvent(eventType: EventType, event: InterventionsEvent) {
    logger.debug("checking flag for notify-action-plan-approved")
    if (featureFlags.crs["notify-action-plan-approved"] != true) return

    logger.debug("processing notify-action-plan-approved")
    val (actionPlan, referral, intervention) = getActionPlanReferralAndIntervention(event)

    val descriptionForNotesField = "Action Plan Approved"
    logger.debug("notifying nDelius of $descriptionForNotesField {} {} {}", kv("actionPlan", actionPlan), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifyActionPlanEvent(eventType, event.occurredAt, referral, intervention, descriptionForNotesField).block()
  }

  private fun getActionPlanReferralAndIntervention(event: InterventionsEvent): Triple<ActionPlan, SentReferral, Intervention> {
    val actionPlan = interventionsApiService.get(URI(event.detailUrl), ActionPlan::class).block()
    val referral = interventionsApiService.getReferral(actionPlan.referralId).block()
    val intervention = interventionsApiService.getIntervention(referral.interventionId).block()
    return Triple(actionPlan, referral, intervention)
  }
}
