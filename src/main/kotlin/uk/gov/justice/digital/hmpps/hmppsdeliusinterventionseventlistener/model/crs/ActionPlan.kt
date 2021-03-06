package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs

import java.time.OffsetDateTime
import java.util.UUID

data class ActionPlan(
  val id: UUID,
  val referralId: UUID,
  val submittedAt: OffsetDateTime,
)
