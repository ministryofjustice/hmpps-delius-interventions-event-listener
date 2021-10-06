package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.config.aws

import com.amazon.sqs.javamessaging.ProviderConfiguration
import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.services.sqs.AmazonSQS
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.listener.DefaultMessageListenerContainer
import uk.gov.justice.hmpps.sqs.HmppsQueueDestinationResolver
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import javax.jms.Session

@Configuration
@EnableJms
class SQSConfiguration {
  companion object {
    const val queueId = "deliusinterventionseventsqueue"
  }

  // the bean name here is important so that this bean overrides the
  // correct 'hmpps-sqs' bean (which does not allow custom configuration)
  @Bean @Qualifier("$queueId-jms-listener-factory")
  fun jmsListenerContainerFactory(
    @Qualifier("$queueId-sqs-client") sqs: AmazonSQS,
    hmppsSqsProperties: HmppsSqsProperties,
  ): DefaultJmsListenerContainerFactory {
    return CustomJmsListenerContainerFactory().apply {
      setConnectionFactory(SQSConnectionFactory(ProviderConfiguration(), sqs))
      setDestinationResolver(HmppsQueueDestinationResolver(hmppsSqsProperties))
      setConcurrency("1-1")
      setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE)
    }
  }
}

class CustomMessageListenerContainer : DefaultMessageListenerContainer() {
  override fun rollbackOnExceptionIfNecessary(session: Session, ex: Throwable) {
    // do nothing (the default listener 'negative acknowledges' the message, zeroing the visibility timeout)
  }
}

class CustomJmsListenerContainerFactory : DefaultJmsListenerContainerFactory() {
  override fun createContainerInstance(): DefaultMessageListenerContainer {
    return CustomMessageListenerContainer()
  }
}
