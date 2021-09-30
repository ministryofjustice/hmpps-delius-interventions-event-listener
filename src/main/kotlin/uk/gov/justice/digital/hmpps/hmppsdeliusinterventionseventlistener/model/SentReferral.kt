package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model

import java.time.OffsetDateTime
import java.util.UUID

data class SentReferral(
  val id: UUID,
  val interventionId: UUID,
  val serviceUserCRN: String,
  val relevantSentenceId: Long,
  val referenceNumber: String,
  val sentAt: OffsetDateTime,
)
