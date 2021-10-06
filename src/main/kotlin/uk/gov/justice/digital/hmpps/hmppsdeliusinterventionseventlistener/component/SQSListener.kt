package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent

@Component
class SQSListener(
  private val objectMapper: ObjectMapper,
  private val eventProcessor: EventProcessor,
) {
  companion object : KLogging()

  @JmsListener(destination = "deliusinterventionseventsqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun listener(rawMessage: String) {
    val notification = objectMapper.readValue(rawMessage, NotificationMessage::class.java)
    val event = objectMapper.readValue(notification.message, InterventionsEvent::class.java)
    eventProcessor.process(event)
  }
}

private data class NotificationMessage(
  @JsonProperty("Message") val message: String
)
