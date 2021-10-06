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
  ACTION_PLAN_SUBMITTED("intervention.action-plan.submitted"),
}

@Component
class EventProcessor(
  private val interventionsApiService: InterventionsApiService,
  private val communityApiService: CommunityApiService,
) {
  companion object : KLogging()

  fun process(event: InterventionsEvent) {
    when (event.eventType) {
      EventType.ACTION_PLAN_SUBMITTED.value -> processActionPlanSubmittedEvent(event)
      else -> return
    }

    logger.debug("event processed successfully {}", kv("event", event))
  }

  fun processActionPlanSubmittedEvent(event: InterventionsEvent) {
    val actionPlan = interventionsApiService.get(URI(event.detailUrl), ActionPlan::class).block()
    val referral = interventionsApiService.getReferral(actionPlan.referralId).block()
    val intervention = interventionsApiService.getIntervention(referral.interventionId).block()

    logger.debug("processing action plan submitted event {} {} {}", kv("actionPlan", actionPlan), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifyActionPlanSubmitted(event.detailUrl, actionPlan, referral, intervention)
  }
}
