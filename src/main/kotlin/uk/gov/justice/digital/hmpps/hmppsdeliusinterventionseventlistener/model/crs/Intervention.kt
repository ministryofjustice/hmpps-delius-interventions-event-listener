package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs

import java.util.UUID

data class Intervention(
  val id: UUID,
  val title: String,
  val description: String,
  val serviceProvider: ServiceProvider,
  val contractType: ContractType,
)
