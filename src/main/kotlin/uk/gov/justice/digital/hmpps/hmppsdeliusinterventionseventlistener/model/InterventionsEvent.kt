package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model

import java.lang.IllegalStateException
import java.time.OffsetDateTime

data class InterventionsEvent(
  val version: Int,
  val eventType: String,
  val description: String,
  val detailUrl: String,
  val occurredAt: OffsetDateTime,
  val additionalInformation: Map<String, String>,
) {
  fun get(name: String) = additionalInformation[name] ?: throw IllegalStateException("Missing additional information: $name")
}
