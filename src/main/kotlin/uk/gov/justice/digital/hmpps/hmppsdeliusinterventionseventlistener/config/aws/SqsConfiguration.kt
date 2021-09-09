package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.config.aws

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.support.NotificationMessageArgumentResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.MessageConverter

@Configuration
class SqsConfiguration {
  @Bean
  fun queueMessageHandlerFactory(messageConverter: MessageConverter): QueueMessageHandlerFactory {
    val factory = QueueMessageHandlerFactory()
    factory.setArgumentResolvers(listOf(NotificationMessageArgumentResolver(messageConverter)))
    return factory
  }

  @Bean
  protected fun messageConverter(): MessageConverter? {
    val converter = MappingJackson2MessageConverter()
    converter.objectMapper.registerKotlinModule()
    converter.objectMapper.registerModule(JavaTimeModule())
    converter.isStrictContentTypeMatch = false
    return converter
  }
}
