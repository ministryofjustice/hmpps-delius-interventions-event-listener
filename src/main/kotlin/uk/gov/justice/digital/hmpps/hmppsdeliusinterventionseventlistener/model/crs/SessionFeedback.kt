package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs

data class SessionFeedback(
  val attendance: Attendance,
  val behaviour: Behaviour
)

data class Attendance(
  val attended: String,
)

data class Behaviour(
  val notifyProbationPractitioner: Boolean?,
)
