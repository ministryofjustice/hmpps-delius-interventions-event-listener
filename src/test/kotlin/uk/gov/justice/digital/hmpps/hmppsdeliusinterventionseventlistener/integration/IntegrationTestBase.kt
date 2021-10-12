package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration

import com.amazonaws.services.sqs.AmazonSQS
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("local", "test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  fun noMessagesCurrentlyOnQueue(client: AmazonSQS, queueUrl: String) {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        client,
        queueUrl
      )
    } matches { it == 0 }
  }

  private fun getNumberOfMessagesCurrentlyOnQueue(sqsClient: AmazonSQS, queueUrl: String): Int? {
    val queueAttributes = sqsClient.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
}
