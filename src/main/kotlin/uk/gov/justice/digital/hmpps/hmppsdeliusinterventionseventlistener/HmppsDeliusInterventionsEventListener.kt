package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsDeliusInterventionsEventListener

fun main(args: Array<String>) {
  runApplication<HmppsDeliusInterventionsEventListener>(*args)
}
