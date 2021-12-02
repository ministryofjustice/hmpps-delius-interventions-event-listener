package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs

import java.time.OffsetDateTime
import java.util.UUID

data class DeliverySession(
  val id: UUID,
  val sessionNumber: Int,
  val appointmentTime: OffsetDateTime,
  val durationInMinutes: Int,
  val sessionFeedback: SessionFeedback,
)
