package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.EventType.ACTION_PLAN_SUBMITTED
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
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

class SQSListenerActionPlanSubmittedTest : IntegrationTestBase() {

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Value("\${hmpps.sqs.topics.interventioneventstopic.arn}")
  lateinit var interventionsEventTopicArn: String

  @MockBean
  lateinit var interventionsApiClient: InterventionsApiClient

  @MockBean
  lateinit var communityApiClient: CommunityApiClient

  @Test
  fun `Message is consumed of queue bound to topic`() {

    // Given
    val serviceProvider = ServiceProvider("SP Name", "55555")
    val intervention = Intervention(UUID.randomUUID(), "Title", "Desc", serviceProvider, ContractType("ACC", "Accommodation"))
    val referral = SentReferral(UUID.randomUUID(), intervention.id, "CRN444", 123L, "ABCDEFGH", OffsetDateTime.now())
    val actionPlan = ActionPlan(UUID.randomUUID(), referral.id, OffsetDateTime.now())
    val interventionsBaseUrl = "http://localhost:8080"

    setUpActionPlanLookup(interventionsBaseUrl, actionPlan)
    setUpReferralLookup(interventionsBaseUrl, referral)
    setUpInterventionsLookup(interventionsBaseUrl, intervention)

    val communityApiBaseUrl = "http://localhost:8091"
    val notificationRequestBody = buildNotificationRequest(intervention, referral, actionPlan)

    setUpCommunityApiCall(communityApiBaseUrl, notificationRequestBody)

    // When
    val message = setUpActionPlanSubmittedInterventionsMessage(interventionsBaseUrl, actionPlan)
    snsClient!!.publish(message)

    // Then
    verifyCommunityApiCall(communityApiBaseUrl, notificationRequestBody)
  }

  @Test
  fun `Message is placed in dead letter queue after successive failures`() {

    // Given
    val serviceProvider = ServiceProvider("SP Name", "55555")
    val intervention = Intervention(UUID.randomUUID(), "Title", "Desc", serviceProvider, ContractType("ACC", "Accommodation"))
    val referral = SentReferral(UUID.randomUUID(), intervention.id, "CRN444", 123L, "ABCDEFGH", OffsetDateTime.now())
    val actionPlan = ActionPlan(UUID.randomUUID(), referral.id, OffsetDateTime.now())
    val interventionsBaseUrl = "http://localhost:8080"

    setUpActionPlanLookupNotFound(interventionsBaseUrl, actionPlan.id)

    // When
    val message = setUpActionPlanSubmittedInterventionsMessage(interventionsBaseUrl, actionPlan)
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

  private fun setUpActionPlanSubmittedInterventionsMessage(interventionsBaseUrl: String, actionPlan: ActionPlan): PublishRequest? {
    val event = InterventionsEvent(1, ACTION_PLAN_SUBMITTED.value, "Description", "$interventionsBaseUrl/action-plan/${actionPlan.id}", OffsetDateTime.now(), emptyMap())
    val messageAttributes = mapOf(
      "eventType" to MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(ACTION_PLAN_SUBMITTED.value)
        .build()
    )
    return PublishRequest.builder()
      .messageAttributes(messageAttributes)
      .message(objectMapper.writeValueAsString(event))
      .topicArn(interventionsEventTopicArn)
      .build()
  }

  private fun buildNotificationRequest(intervention: Intervention, referral: SentReferral, actionPlan: ActionPlan) =
    CreateNotificationRequest(
      intervention.contractType.code,
      referral.sentAt,
      referral.id,
      actionPlan.submittedAt,
      """Action Plan Submitted for ${intervention.contractType.name} Referral ${referral.referenceNumber} with Prime Provider ${intervention.serviceProvider.name}
http://localhost:3000/probation-practitioner/referrals/${referral.id}/action-plan
(notified via delius-interventions-event-listener)""",
    )
}
