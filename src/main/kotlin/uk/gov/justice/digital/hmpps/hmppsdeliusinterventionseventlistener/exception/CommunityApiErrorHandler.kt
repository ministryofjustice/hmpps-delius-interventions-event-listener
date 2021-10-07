package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service.CommunityApiService

@Component
class CommunityApiErrorHandler(private val objectMapper: ObjectMapper) {

  fun handleResponse(e: Throwable, requestUrl: String, requestBody: Any): CommunityApiCallError {
    val responseBodyAsString = when (e) {
      is BadRequest -> e.responseBodyAsString
      else -> e.localizedMessage
    }

    val statusCode = when (e) {
      is WebClientResponseException -> e.statusCode
      else -> INTERNAL_SERVER_ERROR
    }

    val causeMessage = userMessageOrDeveloperMessageOrResponseBodyInThatOrder(responseBodyAsString)
    val error = CommunityApiCallError(statusCode, causeMessage, responseBodyAsString, e)
    CommunityApiService.logger.error(
      "Call to community api failed [${error.category}]",
      e,
      StructuredArguments.kv("req.url", requestUrl),
      StructuredArguments.kv("req.body", requestBody),
      StructuredArguments.kv("res.body", responseBodyAsString),
      StructuredArguments.kv("res.causeMessage", causeMessage)
    )

    return error
  }

  private fun userMessageOrDeveloperMessageOrResponseBodyInThatOrder(responseBody: String): String {
    try {
      objectMapper.readValue(responseBody, ObjectNode::class.java)?.let { node ->
        val userMessage = node.get("userMessage") ?: run {
          val developerMessage = node.get("developerMessage")
          return developerMessage.textValue()
        }
        return userMessage.textValue()
      }
      return responseBody
    } catch (e: JsonProcessingException) {
      // response body does not contain json
      return responseBody
    } catch (e: JsonMappingException) {
      // response body does not contain json
      return responseBody
    }
  }
}
