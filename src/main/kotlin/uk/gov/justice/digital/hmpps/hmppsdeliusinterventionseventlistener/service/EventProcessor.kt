package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crsinterventions.InterventionsEvent

enum class EventType(val value: String) {
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
      else -> logger.info("event type ${event.eventType} not supported")
    }
  }

  fun processActionPlanSubmittedEvent(event: InterventionsEvent) {

    val actionPlan = interventionsApiService.getActionPlan(event.detailUrl)
    val referral = interventionsApiService.getReferral(actionPlan.referralId)
    val intervention = interventionsApiService.getIntervention(referral.interventionId)

    logger.debug("actionPlan=$actionPlan")
    logger.debug("referral=$referral")
    logger.debug("intervention=$intervention")

    communityApiService.notifyActionPlanSubmitted(event.detailUrl, actionPlan, referral, intervention)

    logger.debug("event notified {}", StructuredArguments.kv("event", event))
  }
}
