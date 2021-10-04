package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crsinterventions

import java.time.OffsetDateTime

data class InterventionsEvent(
  val version: Int,
  val eventType: String,
  val description: String,
  val detailUrl: String,
  val occurredAt: OffsetDateTime,
  val additionalInformation: Map<String, String>,
)
