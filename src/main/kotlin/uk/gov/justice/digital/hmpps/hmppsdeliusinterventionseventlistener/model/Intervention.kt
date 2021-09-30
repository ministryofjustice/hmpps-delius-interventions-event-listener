package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model

import java.util.UUID

data class Intervention(
  val id: UUID,
  val title: String,
  val description: String,
  val serviceProvider: ServiceProvider,
  val contractType: ContractType,
)

class ServiceProvider(
  val name: String,
  val id: String
)

data class ContractType(
  val code: String,
  val name: String,
)
