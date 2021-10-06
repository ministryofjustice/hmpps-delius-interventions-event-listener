package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crsinterventions.InterventionsEvent
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.CommunityApiService
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.InterventionsApiService

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
    val actionPlan = interventionsApiService.getActionPlan(event.detailUrl)
    val referral = interventionsApiService.getReferral(actionPlan.referralId)
    val intervention = interventionsApiService.getIntervention(referral.interventionId)

    logger.debug("processing action plan submitted event {} {} {}", kv("actionPlan", actionPlan), kv("referral", referral), kv("intervention", intervention))
    communityApiService.notifyActionPlanSubmitted(event.detailUrl, actionPlan, referral, intervention)
  }
}
