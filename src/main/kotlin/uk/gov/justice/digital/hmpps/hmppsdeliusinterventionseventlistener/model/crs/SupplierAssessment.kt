package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs

import java.util.UUID

data class SupplierAssessment(
  val id: UUID,
  val appointments: List<Appointment>,
  val currentAppointmentId: UUID,
  val referralId: UUID,
) {
  val currentAppointment = appointments.find { it.id == currentAppointmentId }
    ?: throw IllegalStateException("Supplier assessments contains no current appointment")
}
