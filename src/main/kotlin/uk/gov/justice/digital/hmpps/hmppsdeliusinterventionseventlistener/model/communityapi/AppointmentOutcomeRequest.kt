package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi

data class AppointmentOutcomeRequest(
  val notes: String,
  val attended: String,
  val notifyPPOfAttendanceBehaviour: Boolean,
)
