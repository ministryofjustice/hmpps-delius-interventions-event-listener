package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import io.awspring.cloud.messaging.config.annotation.NotificationMessage
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent

@Component
class SQSListener {
  companion object : KLogging()

  @SqsListener(value = ["\${cloud.aws.sqs.queues.interventions-events}"], deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  fun listener(@NotificationMessage event: InterventionsEvent) {
    logger.info("event received {}", StructuredArguments.kv("event", event))
  }
}
