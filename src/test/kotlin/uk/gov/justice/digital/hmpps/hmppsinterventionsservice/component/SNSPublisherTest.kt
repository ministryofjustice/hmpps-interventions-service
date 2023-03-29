package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.EventDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.PersonReference
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import java.time.OffsetDateTime
import java.util.UUID

class SNSPublisherTest {
  private val snsClient = mock<SnsClient>()
  private val objectMapper = ObjectMapper()
  private val telemetryClient = mock<TelemetryClient>()

  private val aReferralId = UUID.fromString("82138d14-3835-442b-b39b-9f8a07650bbe")
  private val aUser = AuthUser("d7c4c3a7a7", "irrelevant", "d7c4c3a7a7@example.org")

  val event = EventDTO(
    eventType = "intervention.test.event",
    description = "A referral has been sent to a Service Provider",
    detailUrl = "http:test/abc",
    occurredAt = OffsetDateTime.parse("2020-12-04T10:42:43.123+00:00"),
    additionalInformation = mapOf("somethingSpecific" to 42),
    personReference = PersonReference.crn("X123456"),
  )

  private fun snsPublisher(enabled: Boolean): SNSPublisher {
    return SNSPublisher(snsClient, objectMapper, telemetryClient, enabled, "arn")
  }

  @Test
  fun `puts eventType into the message attributes so listener can use it for filtering`() {
    snsPublisher(true).publish(aReferralId, aUser, event)

    val requestCaptor = argumentCaptor<PublishRequest>()
    verify(snsClient).publish(requestCaptor.capture())
    assertThat(requestCaptor.firstValue.messageAttributes()["eventType"]!!.dataType()).isEqualTo("String")
    assertThat(requestCaptor.firstValue.messageAttributes()["eventType"]!!.stringValue()).isEqualTo(event.eventType)
  }

  @Test
  fun `serialises the event payload as JSON`() {
    snsPublisher(true).publish(aReferralId, aUser, event)

    val requestCaptor = argumentCaptor<PublishRequest>()
    verify(snsClient).publish(requestCaptor.capture())
    assertThat(requestCaptor.firstValue.message()).isEqualTo(
      """
      {
      "eventType":"intervention.test.event",
      "description":"A referral has been sent to a Service Provider",
      "detailUrl":"http:test/abc",
      "occurredAt":"2020-12-04T10:42:43.123Z",
      "additionalInformation":{"somethingSpecific":42},
      "personReference":{"identifiers":[{"type":"CRN","value":"X123456"}]},
      "version":1
      }
      """.trimIndent().replace("\n", ""),
    )
  }

  @Test
  fun `sends custom event to application insights on publish`() {
    snsPublisher(true).publish(aReferralId, aUser, event)
    verify(telemetryClient).trackEvent(
      "InterventionsDomainEvent",
      mapOf(
        "event" to "intervention.test.event",
        "referralId" to "82138d14-3835-442b-b39b-9f8a07650bbe",
        "actorUserId" to "d7c4c3a7a7",
        "actorUserName" to "d7c4c3a7a7@example.org",
      ),
      null,
    )
  }

  @Test
  fun `thrown exception is not swallowed`() {
    whenever(snsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException::class.java)
    assertThrows<RuntimeException> { snsPublisher(true).publish(aReferralId, aUser, event) }
  }

  @Test
  fun `event does not publish when service is disabled`() {
    snsPublisher(false).publish(aReferralId, aUser, event)
    verifyNoInteractions(snsClient)
  }

  @Test
  fun `event still sends telemetry when service is disabled`() {
    snsPublisher(false).publish(aReferralId, aUser, event)
    verify(telemetryClient).trackEvent(
      "InterventionsDomainEvent",
      mapOf(
        "event" to "intervention.test.event",
        "referralId" to "82138d14-3835-442b-b39b-9f8a07650bbe",
        "actorUserId" to "d7c4c3a7a7",
        "actorUserName" to "d7c4c3a7a7@example.org",
      ),
      null,
    )
  }
}
