package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration.listener

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import reactor.core.publisher.Mono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.InterventionsApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.Contact
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.CreateNotificationRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ContractType
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.EventType
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.EventType.ACTION_PLAN_APPROVED
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.EventType.ACTION_PLAN_SUBMITTED
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

internal class ActionPlanEventsSQSListenerTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("deliusinterventionseventsqueue-sqs-client")
  lateinit var sqsClient: AmazonSQS

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Value("\${hmpps.sqs.topics.interventioneventstopic.arn}")
  lateinit var interventionsEventTopicArn: String

  @Value("\${hmpps.sqs.queues.deliusinterventionseventsqueue.queueName}")
  lateinit var interventionsEventQueue: String

  @Value("\${hmpps.sqs.queues.deliusinterventionseventsqueue.dlqName}")
  lateinit var interventionsEventDeadLetterQueue: String

  @Value("\${hmpps.sqs.region}")
  lateinit var region: String

  @Value("\${hmpps.sqs.localstackUrl}")
  lateinit var localStackUrl: String

  @Value("\${services.interventions-api.baseurl}")
  lateinit var interventionsBaseUrl: String

  @Value("\${services.community-api.baseurl}")
  lateinit var communityApiBaseUrl: String

  @MockBean
  lateinit var interventionsApiClient: InterventionsApiClient

  @MockBean
  lateinit var communityApiClient: CommunityApiClient

  private var snsClient: SnsClient? = null

  private var queueUrl: String? = null

  private var deadLetterQueueUrl: String? = null

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

  @Test
  fun `Action Plan Submitted Message is consumed of queue bound to topic`() {

    // Given
    val serviceProvider = ServiceProvider("SP Name", "55555")
    val intervention = Intervention(UUID.randomUUID(), serviceProvider, ContractType("ACC", "Accommodation"))
    val referral = SentReferral(UUID.randomUUID(), intervention.id, "CRN444", 123L, "ABCDEFGH", OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC))
    val actionPlan = ActionPlan(
      UUID.randomUUID(),
      referral.id,
      OffsetDateTime.of(2021, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC),
      null
    )

    setUpActionPlanLookup(interventionsBaseUrl, actionPlan)
    setUpReferralLookup(interventionsBaseUrl, referral)
    setUpInterventionsLookup(interventionsBaseUrl, intervention)

    val notificationRequestBody = buildActionPlanNotificationRequest("Action Plan Submitted", intervention, referral, actionPlan.submittedAt)

    setUpCommunityApiCall(communityApiBaseUrl, notificationRequestBody)

    // When
    val message = setUpActionPlanInterventionsMessage(ACTION_PLAN_SUBMITTED, actionPlan.submittedAt, interventionsBaseUrl, actionPlan)
    snsClient!!.publish(message)

    // Then
    verifyCommunityApiCall(communityApiBaseUrl, notificationRequestBody)
  }

  @Test
  fun `Action Plan Approved Message is consumed of queue bound to topic`() {

    // Given
    val serviceProvider = ServiceProvider("SP Name", "55555")
    val intervention = Intervention(UUID.randomUUID(), serviceProvider, ContractType("ACC", "Accommodation"))
    val referral = SentReferral(UUID.randomUUID(), intervention.id, "CRN444", 123L, "ABCDEFGH", OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC))
    val actionPlan = ActionPlan(
      UUID.randomUUID(),
      referral.id,
      OffsetDateTime.of(2021, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC),
      OffsetDateTime.of(2021, 3, 3, 3, 3, 3, 3, ZoneOffset.UTC),
    )

    setUpActionPlanLookup(interventionsBaseUrl, actionPlan)
    setUpReferralLookup(interventionsBaseUrl, referral)
    setUpInterventionsLookup(interventionsBaseUrl, intervention)

    val notificationRequestBody = buildActionPlanNotificationRequest("Action Plan Approved", intervention, referral, actionPlan.approvedAt!!)

    setUpCommunityApiCall(communityApiBaseUrl, notificationRequestBody)

    // When
    val message = setUpActionPlanInterventionsMessage(ACTION_PLAN_APPROVED, actionPlan.approvedAt!!, interventionsBaseUrl, actionPlan)
    snsClient!!.publish(message)

    // Then
    verifyCommunityApiCall(communityApiBaseUrl, notificationRequestBody)
  }

  @Test
  fun `Message is placed in dead letter queue after successive failures`() {

    // Given
    val serviceProvider = ServiceProvider("SP Name", "55555")
    val intervention = Intervention(UUID.randomUUID(), serviceProvider, ContractType("ACC", "Accommodation"))
    val referral = SentReferral(UUID.randomUUID(), intervention.id, "CRN444", 123L, "ABCDEFGH", OffsetDateTime.now())
    val actionPlan = ActionPlan(
      UUID.randomUUID(),
      referral.id,
      OffsetDateTime.of(2021, 2, 2, 2, 2, 2, 2, ZoneOffset.UTC),
      null
    )

    setUpActionPlanLookupNotFound(interventionsBaseUrl, actionPlan.id)

    // When
    val message = setUpActionPlanInterventionsMessage(ACTION_PLAN_SUBMITTED, actionPlan.submittedAt, interventionsBaseUrl, actionPlan)
    snsClient!!.publish(message)

    // Then
    verifyMessageOnDeadLetterQueueAfterFailure()
  }

  private fun verifyCommunityApiCall(communityApiBaseUrl: String, expectedNotificationRequestBody: CreateNotificationRequest) {
    noMessagesCurrentlyOnQueue(sqsClient, queueUrl!!)
    noMessagesCurrentlyOnQueue(sqsClient, deadLetterQueueUrl!!)

    verify(communityApiClient).post(
      eq(URI.create("$communityApiBaseUrl/secure/offenders/crn/CRN444/sentences/123/notifications/context/commissioned-rehabilitation-services")),
      eq(expectedNotificationRequestBody),
      eq(Contact::class)
    )
  }

  private fun verifyMessageOnDeadLetterQueueAfterFailure() {
    verifyZeroInteractions(communityApiClient)
    noMessagesCurrentlyOnQueue(sqsClient, queueUrl!!)
    oneMessageCurrentlyOnQueue(sqsClient, deadLetterQueueUrl!!)
  }

  private fun setUpActionPlanLookup(interventionsBaseUrl: String, actionPlan: ActionPlan) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/action-plan/${actionPlan.id}")), eq(ActionPlan::class))).thenReturn(Mono.just(actionPlan))
  }

  private fun setUpActionPlanLookupNotFound(interventionsBaseUrl: String, actionPlanId: UUID) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/action-plan/$actionPlanId")), eq(ActionPlan::class))).thenReturn(Mono.empty())
  }

  private fun setUpReferralLookup(interventionsBaseUrl: String, referral: SentReferral) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/sent-referral/${referral.id}")), eq(SentReferral::class))).thenReturn(Mono.just(referral))
  }

  private fun setUpInterventionsLookup(interventionsBaseUrl: String, intervention: Intervention) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/intervention/${intervention.id}")), eq(Intervention::class))).thenReturn(Mono.just(intervention))
  }

  private fun setUpCommunityApiCall(communityApiBaseUrl: String, createNotificationRequest: CreateNotificationRequest) {
    whenever(
      communityApiClient.post(
        eq(URI.create("$communityApiBaseUrl/secure/offenders/crn/CRN444/sentences/123/notifications/context/commissioned-rehabilitation-services")),
        eq(createNotificationRequest),
        eq(Contact::class)
      )
    ).thenReturn(Mono.just(Contact(999L)))
  }

  private fun setUpActionPlanInterventionsMessage(eventType: EventType, occurredAt: OffsetDateTime, interventionsBaseUrl: String, actionPlan: ActionPlan): PublishRequest? {
    val event = InterventionsEvent(1, eventType.eventTypeValue, "Description", "$interventionsBaseUrl/action-plan/${actionPlan.id}", occurredAt, emptyMap())
    val messageAttributes = mapOf(
      "eventType" to MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(eventType.eventTypeValue)
        .build()
    )
    return PublishRequest.builder()
      .messageAttributes(messageAttributes)
      .message(objectMapper.writeValueAsString(event))
      .topicArn(interventionsEventTopicArn)
      .build()
  }

  private fun buildActionPlanNotificationRequest(eventTypeDescription: String, intervention: Intervention, referral: SentReferral, occurredAt: OffsetDateTime) =
    CreateNotificationRequest(
      intervention.contractType.code,
      referral.sentAt,
      referral.id,
      occurredAt,
      """$eventTypeDescription for ${intervention.contractType.name} Referral ${referral.referenceNumber} with Prime Provider ${intervention.serviceProvider.name}
http://localhost:3000/probation-practitioner/referrals/${referral.id}/action-plan
(notified via delius-interventions-event-listener)""",
    )
}
