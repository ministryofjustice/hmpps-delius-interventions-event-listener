package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.communityapi

import java.time.OffsetDateTime
import java.util.UUID

data class CreateNotificationRequest(
  val contractType: String,
  val referralStart: OffsetDateTime,
  val referralId: UUID,
  val contactDateTime: OffsetDateTime,
  val notes: String
)
