package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration.listener

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.EventType.DELIVERY_SESSION_FEEDBACK_SUBMITTED
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.InterventionsApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.AppointmentOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi.Contact
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Attendance
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Behaviour
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ContractType
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.DeliverySession
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SessionFeedback
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

class SQSListenerSessionDeliveryOutcomeTest : IntegrationTestBase() {

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
    val sessionFeedback = SessionFeedback(Attendance("yes"), Behaviour(false))
    val deliverySession = DeliverySession(UUID.randomUUID(), 1, OffsetDateTime.now(), 60, sessionFeedback)
    val interventionsBaseUrl = "http://localhost:8080"

    setUpDeliverySessionLookup(interventionsBaseUrl, referral.id, deliverySession)
    setUpReferralLookup(interventionsBaseUrl, referral)
    setUpInterventionsLookup(interventionsBaseUrl, intervention)

    val communityApiBaseUrl = "http://localhost:8091"
    val appointmentOutcomeRequestBody = buildAppointmentOutcomeRequest(intervention, referral, deliverySession.sessionNumber, sessionFeedback)
    val deliusAppointmentId = "999"

    setUpCommunityApiCall(communityApiBaseUrl, deliusAppointmentId, appointmentOutcomeRequestBody)

    // When
    val message = setUpSessionFeedbackSubmittedInterventionsMessage(interventionsBaseUrl, referral.id, deliverySession.sessionNumber, deliusAppointmentId)
    snsClient!!.publish(message)

    // Then
    verifyCommunityApiCall(communityApiBaseUrl, deliusAppointmentId, appointmentOutcomeRequestBody)
  }

  @Test
  fun `Message is placed in dead letter queue after successive failures`() {

    // Given
    val serviceProvider = ServiceProvider("SP Name", "55555")
    val intervention = Intervention(UUID.randomUUID(), "Title", "Desc", serviceProvider, ContractType("ACC", "Accommodation"))
    val referral = SentReferral(UUID.randomUUID(), intervention.id, "CRN444", 123L, "ABCDEFGH", OffsetDateTime.now())
    val sessionFeedback = SessionFeedback(Attendance("yes"), Behaviour(false))
    val deliverySession = DeliverySession(UUID.randomUUID(), 1, OffsetDateTime.now(), 60, sessionFeedback)
    val interventionsBaseUrl = "http://localhost:8080"

    setUpDeliverySessionLookupNotFound(interventionsBaseUrl, referral.id, deliverySession)
    val deliusAppointmentId = "999"

    // When
    val message = setUpSessionFeedbackSubmittedInterventionsMessage(interventionsBaseUrl, referral.id, deliverySession.sessionNumber, deliusAppointmentId)
    snsClient!!.publish(message)

    // Then
    verifyMessageOnDeadLetterQueueAfterFailure()
  }

  private fun verifyCommunityApiCall(communityApiBaseUrl: String, deliusAppointmentId: String, appointmentOutcomeRequest: AppointmentOutcomeRequest) {
    noMessagesCurrentlyOnQueue(sqsClient, queueUrl!!)
    noMessagesCurrentlyOnQueue(sqsClient, deadLetterQueueUrl!!)

    verify(communityApiClient).post(
      eq(URI.create("$communityApiBaseUrl/secure/offenders/crn/CRN444/appointments/$deliusAppointmentId/outcome/context/commissioned-rehabilitation-services")),
      eq(appointmentOutcomeRequest),
      eq(Contact::class)
    )
  }

  private fun verifyMessageOnDeadLetterQueueAfterFailure() {
    verifyNoInteractions(communityApiClient)
    noMessagesCurrentlyOnQueue(sqsClient, queueUrl!!)
    oneMessageCurrentlyOnQueue(sqsClient, deadLetterQueueUrl!!)
  }

  private fun setUpDeliverySessionLookup(interventionsBaseUrl: String, referralId: UUID, deliverySession: DeliverySession) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/sent-referral/$referralId/sessions/${deliverySession.sessionNumber}")), eq(DeliverySession::class))).thenReturn(Mono.just(deliverySession))
  }

  private fun setUpDeliverySessionLookupNotFound(interventionsBaseUrl: String, referralId: UUID, deliverySession: DeliverySession) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/sent-referral/$referralId/sessions/${deliverySession.sessionNumber}")), eq(DeliverySession::class))).thenReturn(Mono.empty())
  }

  private fun setUpReferralLookup(interventionsBaseUrl: String, referral: SentReferral) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/sent-referral/${referral.id}")), eq(SentReferral::class))).thenReturn(Mono.just(referral))
  }

  private fun setUpInterventionsLookup(interventionsBaseUrl: String, intervention: Intervention) {
    whenever(interventionsApiClient.get(eq(URI.create("$interventionsBaseUrl/intervention/${intervention.id}")), eq(Intervention::class))).thenReturn(Mono.just(intervention))
  }

  private fun setUpCommunityApiCall(communityApiBaseUrl: String, deliusAppointmentId: String, appointmentOutcomeRequest: AppointmentOutcomeRequest) {
    whenever(
      communityApiClient.post(
        eq(URI.create("$communityApiBaseUrl/secure/offenders/crn/CRN444/appointments/$deliusAppointmentId/outcome/context/commissioned-rehabilitation-services")),
        eq(appointmentOutcomeRequest),
        eq(Contact::class)
      )
    ).thenReturn(Mono.just(Contact(999L)))
  }

  private fun setUpSessionFeedbackSubmittedInterventionsMessage(interventionsBaseUrl: String, referralId: UUID, sessionNumber: Int, deliusAppointmentId: String): PublishRequest? {
    val additionalInformation = mapOf("referralId" to referralId.toString(), "deliusAppointmentId" to deliusAppointmentId)
    val event = InterventionsEvent(1, DELIVERY_SESSION_FEEDBACK_SUBMITTED.value, "Description", "$interventionsBaseUrl/sent-referral/$referralId/sessions/$sessionNumber", OffsetDateTime.now(), additionalInformation)
    val messageAttributes = mapOf(
      "eventType" to MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(DELIVERY_SESSION_FEEDBACK_SUBMITTED.value)
        .build()
    )
    return PublishRequest.builder()
      .messageAttributes(messageAttributes)
      .message(objectMapper.writeValueAsString(event))
      .topicArn(interventionsEventTopicArn)
      .build()
  }

  private fun buildAppointmentOutcomeRequest(intervention: Intervention, referral: SentReferral, sessionNumber: Int, sessionFeedback: SessionFeedback) =
    AppointmentOutcomeRequest(
      """Session Feedback Recorded for ${intervention.contractType.name} Referral ${referral.referenceNumber} with Prime Provider ${intervention.serviceProvider.name}
http://localhost:3000/probation-practitioner/referrals/${referral.id}/appointment/$sessionNumber/post-session-feedback
(notified via delius-interventions-event-listener)""",
      sessionFeedback.attendance.attended,
      sessionFeedback.behaviour.notifyProbationPractitioner!!
    )
}
