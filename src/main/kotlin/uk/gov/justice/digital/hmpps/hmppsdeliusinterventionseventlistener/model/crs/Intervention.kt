package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs

import java.util.UUID

data class Intervention(
  val id: UUID,
  val serviceProvider: ServiceProvider,
  val contractType: ContractType,
)
