package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import java.net.URI

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("local", "test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Value("\${hmpps.sqs.queues.deliusinterventionseventsqueue.queueName}")
  lateinit var interventionsEventQueue: String

  @Value("\${hmpps.sqs.queues.deliusinterventionseventsqueue.dlqName}")
  lateinit var interventionsEventDeadLetterQueue: String

  @Value("\${hmpps.sqs.region}")
  lateinit var region: String

  @Value("\${hmpps.sqs.localstackUrl}")
  lateinit var localStackUrl: String

  @Autowired
  @Qualifier("deliusinterventionseventsqueue-sqs-client")
  lateinit var sqsClient: AmazonSQS

  var snsClient: SnsClient? = null

  var queueUrl: String? = null

  var deadLetterQueueUrl: String? = null

  @BeforeEach
  fun beforeEach() {
    snsClient = SnsClient.builder()
      .region(Region.of(region))
      .credentialsProvider { AwsBasicCredentials.create("test", "test") }
      .endpointOverride(URI(localStackUrl))
      .build()
    queueUrl = sqsClient.getQueueUrl(interventionsEventQueue).queueUrl
    sqsClient.purgeQueue(PurgeQueueRequest(queueUrl))
    deadLetterQueueUrl = sqsClient.getQueueUrl(interventionsEventDeadLetterQueue).queueUrl
    sqsClient.purgeQueue(PurgeQueueRequest(deadLetterQueueUrl))
  }

  fun noMessagesCurrentlyOnQueue(client: AmazonSQS, queueUrl: String) {
    messagesCurrentlyOnQueue(client, queueUrl, 0)
  }

  fun oneMessageCurrentlyOnQueue(client: AmazonSQS, queueUrl: String) {
    messagesCurrentlyOnQueue(client, queueUrl, 1)
  }

  fun messagesCurrentlyOnQueue(client: AmazonSQS, queueUrl: String, expectedNumber: Int) {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        client,
        queueUrl
      )
    } matches { it == expectedNumber }
  }

  private fun getNumberOfMessagesCurrentlyOnQueue(sqsClient: AmazonSQS, queueUrl: String): Int? {
    val queueAttributes = sqsClient.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
}
