package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration.eventprocessor

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.EventProcessor
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.EventType
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.InterventionsEvent
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ActionPlan
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ContractType
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Intervention
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SentReferral
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.CommunityApiService
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.InterventionsApiService
import java.time.OffsetDateTime
import java.util.UUID

class EventProcessorIntTest @Autowired constructor(
  private val eventProcessor: EventProcessor,
) : IntegrationTestBase() {
  @MockBean lateinit var interventionsApiService: InterventionsApiService
  @MockBean lateinit var communityApiService: CommunityApiService

  @Test
  fun `ACTION_PLAN_SUBMITTED event notifies nDelius about the new action plan`() {
    val serviceProvider = ServiceProvider("name", "123")
    val intervention = Intervention(UUID.randomUUID(), "title", "description", serviceProvider, ContractType("", "Accommodation"))
    val referral = SentReferral(UUID.randomUUID(), intervention.id, "CRN123", 123L, "123", OffsetDateTime.now())
    val actionPlan = ActionPlan(UUID.randomUUID(), referral.id, OffsetDateTime.now())
    whenever(interventionsApiService.get(any(), eq(ActionPlan::class))).thenReturn(Mono.just(actionPlan))
    whenever(interventionsApiService.getIntervention(any())).thenReturn(Mono.just(intervention))
    whenever(interventionsApiService.getReferral(any())).thenReturn(Mono.just(referral))

    val event = InterventionsEvent(
      1,
      EventType.ACTION_PLAN_SUBMITTED.value,
      "description",
      "https://interventions.gov.uk/action-plan/123",
      OffsetDateTime.now(),
      emptyMap(),
    )
    eventProcessor.process(event)

    verify(communityApiService).notifyActionPlanSubmitted(
      "https://interventions.gov.uk/action-plan/123",
      actionPlan,
      referral,
      intervention,
    )
  }
}
