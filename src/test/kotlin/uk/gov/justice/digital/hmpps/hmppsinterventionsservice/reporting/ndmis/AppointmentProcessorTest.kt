
package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.QueryLoader
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance.AppointmentProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance.AppointmentReason
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance.ReferralMetadata
import java.time.Instant
import java.util.*

internal class AppointmentProcessorTest {
  private val entityManager = mock<EntityManager>()
  private val queryLoader = mock<QueryLoader>()
  private val processor = AppointmentProcessor(entityManager, queryLoader)

  private val referralId = UUID.randomUUID()
  private val referralReference = "REF123"

  @Test
  fun `referrals with multiple delivery session and saa appointments can be processed`() {
    val referralMetadata = ReferralMetadata(referralId, referralReference)

    val mockQuery = mock<Query>()
    val queryString = "select * from appointment where referral_id = :referralId"
    val now = Instant.now()
    val appointmentResults = listOf(
      arrayOf(
        referralReference,
        referralId,
        UUID.randomUUID(),
        now,
        60,
        now,
        "YES",
        now,
        true,
        "delius123",
        "DELIVERY",
      ),
      arrayOf(
        referralReference,
        referralId,
        UUID.randomUUID(),
        now,
        60,
        now,
        "YES",
        now,
        true,
        "delius124",
        "DELIVERY",
      ),
      arrayOf(
        referralReference,
        referralId,
        UUID.randomUUID(),
        now,
        60,
        now,
        "NO",
        now,
        true,
        "delius125",
        "DELIVERY",
      ),
      arrayOf(
        referralReference,
        referralId,
        UUID.randomUUID(),
        now,
        60,
        now,
        "YES",
        now,
        true,
        "delius126",
        "SAA",
      ),
      arrayOf(
        referralReference,
        referralId,
        UUID.randomUUID(),
        now,
        60,
        now,
        "LATE",
        now,
        true,
        "delius127",
        "SAA",
      ),
    )

    whenever(queryLoader.loadQuery(anyString())).thenReturn(queryString)
    whenever(entityManager.createNativeQuery(eq(queryString))).thenReturn(mockQuery)
    whenever(mockQuery.setParameter("referralId", referralId)).thenReturn(mockQuery)
    whenever(mockQuery.resultList).thenReturn(appointmentResults as List<Any>)

    val result = processor.process(referralMetadata)

    assertThat(result!!.size).isEqualTo(5)
    assertThat(result[0].reasonForAppointment).isEqualTo(AppointmentReason.DELIVERY)
    assertThat(result[1].reasonForAppointment).isEqualTo(AppointmentReason.DELIVERY)
    assertThat(result[2].reasonForAppointment).isEqualTo(AppointmentReason.DELIVERY)
    assertThat(result[3].reasonForAppointment).isEqualTo(AppointmentReason.SAA)
    assertThat(result[4].reasonForAppointment).isEqualTo(AppointmentReason.SAA)
  }

  @Test
  fun `referrals with no saa appointments and no delivery sessions can be processed`() {
    val referralMetadata = ReferralMetadata(referralId, referralReference)

    val mockQuery = mock<Query>()
    val queryString = "select * from appointment where referral_id = :referralId"

    whenever(queryLoader.loadQuery(anyString())).thenReturn(queryString)
    whenever(entityManager.createNativeQuery(eq(queryString))).thenReturn(mockQuery)
    whenever(mockQuery.setParameter("referralId", referralId)).thenReturn(mockQuery)
    whenever(mockQuery.resultList).thenReturn(emptyList<Any>())

    val result = processor.process(referralMetadata)
    assertThat(result).isNull()
  }

  @Test
  fun `referrals with saa appointments and no delivery sessions can be processed`() {
    val referralMetadata = ReferralMetadata(referralId, referralReference)

    val mockQuery = mock<Query>()
    val queryString = "select * from appointment where referral_id = :referralId"
    val now = Instant.now()
    val appointmentResults = listOf(
      arrayOf(
        referralReference,
        referralId,
        UUID.randomUUID(),
        now,
        60,
        now,
        "YES",
        now,
        true,
        "delius128",
        "SAA",
      ),
      arrayOf(
        referralReference,
        referralId,
        UUID.randomUUID(),
        now,
        60,
        now,
        "YES",
        now,
        true,
        "delius129",
        "SAA",
      ),
    )

    whenever(queryLoader.loadQuery(anyString())).thenReturn(queryString)
    whenever(entityManager.createNativeQuery(eq(queryString))).thenReturn(mockQuery)
    whenever(mockQuery.setParameter("referralId", referralId)).thenReturn(mockQuery)
    whenever(mockQuery.resultList).thenReturn(appointmentResults as List<Any>)

    val result = processor.process(referralMetadata)

    assertThat(result!!.size).isEqualTo(2)
    assertThat(result[0].reasonForAppointment).isEqualTo(AppointmentReason.SAA)
    assertThat(result[1].reasonForAppointment).isEqualTo(AppointmentReason.SAA)
  }
}
