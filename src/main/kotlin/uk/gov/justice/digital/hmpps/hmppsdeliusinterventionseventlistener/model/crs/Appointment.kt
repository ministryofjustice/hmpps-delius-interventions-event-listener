package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs

import java.time.OffsetDateTime
import java.util.UUID

data class Appointment(
  val id: UUID,
  val appointmentTime: OffsetDateTime,
  val durationInMinutes: Int,
  val sessionFeedback: SessionFeedback,
)
