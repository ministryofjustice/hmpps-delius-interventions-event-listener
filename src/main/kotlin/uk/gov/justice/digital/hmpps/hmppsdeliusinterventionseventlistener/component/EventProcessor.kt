package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.CommunityApiService
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.InterventionsApiService
import java.net.URI

private enum class EventType(val value: String) {
  ACTION_PLAN_SUBMITTED("intervention.action-plan.submitted");

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
    }
  }

  fun processActionPlanSubmittedEvent(event: InterventionsEvent) {
    if (featureFlags.crs["notify-action-plan-submitted"] != true) return

    val actionPlan = interventionsApiService.get(URI(event.detailUrl), ActionPlan::class).block()
    val referral = interventionsApiService.getReferral(actionPlan.referralId).block()
    val intervention = interventionsApiService.getIntervention(referral.interventionId).block()

    logger.debug("notifying nDelius of submitted action plan {} {} {}", kv("actionPlan", actionPlan), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifyActionPlanSubmitted(event.detailUrl, actionPlan, referral, intervention)
  }
}
