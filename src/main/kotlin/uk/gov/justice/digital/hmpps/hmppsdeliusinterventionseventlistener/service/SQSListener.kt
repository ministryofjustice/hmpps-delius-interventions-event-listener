package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy
import org.springframework.stereotype.Service
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.messaging.support.GenericMessage

@Service
class SQSListener() {
  companion object : KLogging()

  @SqsListener(value = ["\${cloud.aws.sqs.queue.name}"], deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  fun listener(payload: GenericMessage<String>) {
    logger.info("message received {}", StructuredArguments.kv("payload", payload))
  }
}
