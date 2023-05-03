package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.CommunityAPIClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RamDeliusClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SERVICE_DELIVERY
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.LATE
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.YES
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentDeliveryFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DynamicFrameworkContractFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.InterventionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now

internal class CommunityAPIBookingServiceTest {
  private val referralFactory = ReferralFactory()
  private val interventionFactory = InterventionFactory()
  private val dynamicFrameworkContractFactory = DynamicFrameworkContractFactory()
  private val appointmentFactory = AppointmentFactory()
  private val appointmentDeliveryFactory = AppointmentDeliveryFactory()

  private val referral = referralFactory.createSent(
    serviceUserCRN = "X1",
    relevantSentenceId = 123L,
    referenceNumber = "XX123456",
    intervention = interventionFactory.create(
      contract = dynamicFrameworkContractFactory.create(primeProvider = ServiceProvider("SPN", "SPN")),
    ),
  )

  private val httpBaseUrl = "http://url"
  private val progressUrl = "/pp/{referralId}/progress"
  private val supplierAssessmentUrl = "/pp/{referralId}/supplier-assessment"
  private val rescheduleApiUrl = "/appt/{CRN}/{sentenceId}/{contextName}/reschedule"
  private val crsOfficeLocation = "CRSEXTL"
  private val crsBookingsContext = "CRS"
  private val appointmentMergeUrl = "/probation-case/{crn}/referrals/{referralId}/appointments"

  private val communityAPIClient: CommunityAPIClient = mock()
  private val ramDeliusAPIClient: RamDeliusClient = mock()

  private val communityAPIBookingService = CommunityAPIBookingService(
    true,
    httpBaseUrl,
    progressUrl,
    supplierAssessmentUrl,
    rescheduleApiUrl,
    crsOfficeLocation,
    mapOf(SERVICE_DELIVERY to "Service Delivery"),
    mapOf(SERVICE_DELIVERY to true),
    crsBookingsContext,
    communityAPIClient,
    appointmentMergeUrl,
    ramDeliusAPIClient,
  )

  @Nested
  inner class BookingAppointment {

    @Test
    fun `requests booking for a new appointment`() {
      val now = now()
      val uri = "/probation-case/${referral.serviceUserCRN}/referrals/${referral.id}/appointments"
      whenever(ramDeliusAPIClient.makePutAppointmentRequest(eq(uri), any())).thenReturn(AppointmentResponseDTO(1234L))

      val res = communityAPIBookingService.book(
        referral,
        null,
        now.plusMinutes(60),
        60,
        SERVICE_DELIVERY,
        "CRS0001",
        null,
        null,
      )

      val captured = argumentCaptor<AppointmentMerge>()
      verify(ramDeliusAPIClient).makePutAppointmentRequest(eq(uri), captured.capture())

      assertThat(res.first).isEqualTo(1234L)

      val appointmentMerge = captured.firstValue
      assertThat(appointmentMerge.serviceUserCrn).isEqualTo(referral.serviceUserCRN)
      assertThat(appointmentMerge.referralId).isEqualTo(referral.id)
      assertThat(appointmentMerge.officeLocationCode).isEqualTo("CRS0001")

      assertThat(res.first).isEqualTo(1234L)
    }

    @Test
    fun `requests booking for a new appointment without an office location uses default`() {
      val now = now()
      val uri = "/probation-case/${referral.serviceUserCRN}/referrals/${referral.id}/appointments"
      val notes = "Service Delivery Appointment for Accommodation Referral XX123456 with Prime Provider SPN\n" +
        "http://url/pp/${referral.id}/progress"

      whenever(ramDeliusAPIClient.makePutAppointmentRequest(eq(uri), any())).thenReturn(AppointmentResponseDTO(1234L))

      val res = communityAPIBookingService.book(referral, null, now.plusMinutes(60), 60, SERVICE_DELIVERY, null, null, null)

      val captured = argumentCaptor<AppointmentMerge>()
      verify(ramDeliusAPIClient).makePutAppointmentRequest(eq(uri), captured.capture())

      assertThat(res.first).isEqualTo(1234L)

      val appointmentMerge = captured.firstValue
      assertThat(appointmentMerge.serviceUserCrn).isEqualTo(referral.serviceUserCRN)
      assertThat(appointmentMerge.referralId).isEqualTo(referral.id)
      assertThat(appointmentMerge.officeLocationCode).isEqualTo(crsOfficeLocation)
      assertThat(appointmentMerge.notes).isEqualTo(notes)
    }

    @Test
    fun `requests booking for a new historic appointment with attendance and behaviour`() {
      val now = now()

      val uri = "/probation-case/${referral.serviceUserCRN}/referrals/${referral.id}/appointments"
      val notes = "Service Delivery Appointment for Accommodation Referral XX123456 with Prime Provider SPN\n" +
        "http://url/pp/${referral.id}/progress"

      whenever(ramDeliusAPIClient.makePutAppointmentRequest(eq(uri), any())).thenReturn(AppointmentResponseDTO(1234L))

      val res = communityAPIBookingService.book(referral, null, now.plusMinutes(60), 60, SERVICE_DELIVERY, null, YES, true)

      val captured = argumentCaptor<AppointmentMerge>()
      verify(ramDeliusAPIClient).makePutAppointmentRequest(eq(uri), captured.capture())

      assertThat(res.first).isEqualTo(1234L)

      val appointmentMerge = captured.firstValue
      assertThat(appointmentMerge.serviceUserCrn).isEqualTo(referral.serviceUserCRN)
      assertThat(appointmentMerge.referralId).isEqualTo(referral.id)
      assertThat(appointmentMerge.officeLocationCode).isEqualTo(crsOfficeLocation)
      assertThat(appointmentMerge.notes).isEqualTo(notes)
      assertThat(appointmentMerge.outcome).isNotNull
      assertThat(appointmentMerge.outcome!!.attended).isEqualTo(YES)
      assertThat(appointmentMerge.outcome!!.notify).isTrue()
    }

    @Test
    fun `requests booking for a new historic appointment with non attendance resulting notifying the PP`() {
      val now = now()

      val uri = "/probation-case/${referral.serviceUserCRN}/referrals/${referral.id}/appointments"
      val notes = "Service Delivery Appointment for Accommodation Referral XX123456 with Prime Provider SPN\n" +
        "http://url/pp/${referral.id}/progress"

      whenever(ramDeliusAPIClient.makePutAppointmentRequest(eq(uri), any())).thenReturn(AppointmentResponseDTO(1234L))

      val res = communityAPIBookingService.book(referral, null, now.plusMinutes(60), 60, SERVICE_DELIVERY, null, NO, null)

      val captured = argumentCaptor<AppointmentMerge>()
      verify(ramDeliusAPIClient).makePutAppointmentRequest(eq(uri), captured.capture())

      assertThat(res.first).isEqualTo(1234L)

      val appointmentMerge = captured.firstValue
      assertThat(appointmentMerge.serviceUserCrn).isEqualTo(referral.serviceUserCRN)
      assertThat(appointmentMerge.referralId).isEqualTo(referral.id)
      assertThat(appointmentMerge.officeLocationCode).isEqualTo(crsOfficeLocation)
      assertThat(appointmentMerge.notes).isEqualTo(notes)
      assertThat(appointmentMerge.outcome).isNotNull
      assertThat(appointmentMerge.outcome!!.attended).isEqualTo(NO)
      assertThat(appointmentMerge.outcome!!.notify).isTrue()
    }

    @Test
    fun `set notify PP if PoP does not attend`() {
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(NO, null)).isTrue
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(NO, false)).isTrue
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(NO, true)).isTrue
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(YES, null)).isFalse
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(YES, false)).isFalse
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(YES, true)).isTrue
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(LATE, null)).isFalse
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(LATE, false)).isFalse
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(LATE, true)).isTrue
      assertThat(communityAPIBookingService.setNotifyPPIfAttendedNo(null, null)).isFalse
    }
  }

  @Nested
  inner class ReschedulingAppointment {

    @Test
    fun `requests rescheduling an appointment when timings are different`() {
      val now = now()
      val deliusAppointmentId = 999L
      val appointment = makeAppointment(now, now.plusDays(1), 60, deliusAppointmentId)

      val uri = "/appt/X1/999/CRS/reschedule"
      val request = AppointmentRescheduleRequestDTO(now, now.plusMinutes(45), true, crsOfficeLocation)
      val response = AppointmentResponseDTO(1234L)

      whenever(communityAPIClient.makeSyncPostRequest(uri, request, AppointmentResponseDTO::class.java))
        .thenReturn(response)

      communityAPIBookingService.book(referral, appointment, now, 45, SERVICE_DELIVERY, null, null, null)

      verify(communityAPIClient).makeSyncPostRequest(uri, request, AppointmentResponseDTO::class.java)
    }

    @Test
    fun `requests rescheduling an appointment when timings are different and location is different`() {
      val now = now()
      val deliusAppointmentId = 999L
      val existingAppointment = makeAppointment(now, now, 60, deliusAppointmentId)
      val appointmentDelivery = appointmentDeliveryFactory.create(
        existingAppointment.id,
        npsOfficeCode = "OLD_CODE",
        appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE,
      )
      existingAppointment.appointmentDelivery = appointmentDelivery

      val uri = "/appt/X1/999/CRS/reschedule"
      val request = AppointmentRescheduleRequestDTO(now, now.plusMinutes(45), true, "NEW_CODE")
      val response = AppointmentResponseDTO(1234L)

      whenever(communityAPIClient.makeSyncPostRequest(uri, request, AppointmentResponseDTO::class.java))
        .thenReturn(response)

      communityAPIBookingService.book(referral, existingAppointment, now, 45, SERVICE_DELIVERY, "NEW_CODE", null, null)

      verify(communityAPIClient).makeSyncPostRequest(uri, request, AppointmentResponseDTO::class.java)
    }

    @Test
    fun `does not reschedule an appointment when timings are same`() {
      val now = now()
      val deliusAppointmentId = 999L
      val appointment = makeAppointment(now, now.plusDays(1), 60, deliusAppointmentId)

      communityAPIBookingService.book(referral, appointment, now.plusDays(1), 60, SERVICE_DELIVERY, null, null, null)

      verifyNoMoreInteractions(communityAPIClient)
    }

    @Test
    fun `is reschedule`() {
      val referralStart = now()
      val appointmentStart = referralStart.plusDays(1)
      assertThat(communityAPIBookingService.isRescheduleBooking(makeAppointment(referralStart, appointmentStart, 60), appointmentStart, 60, null)).isFalse
      assertThat(communityAPIBookingService.isRescheduleBooking(makeAppointment(referralStart, appointmentStart, 59), appointmentStart, 60, null)).isTrue
      assertThat(communityAPIBookingService.isRescheduleBooking(makeAppointment(referralStart, appointmentStart, 60), appointmentStart.plusNanos(1), 60, null)).isTrue
    }

    @Test
    fun `does not reschedule or relocate an appointment when locations and timings are the same`() {
      val now = now()
      val deliusAppointmentId = 999L
      val existingAppointment = makeAppointment(now, now.plusDays(1), 60, deliusAppointmentId)
      val appointmentDelivery = appointmentDeliveryFactory.create(existingAppointment.id, npsOfficeCode = "NPS_CODE", appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE)
      existingAppointment.appointmentDelivery = appointmentDelivery

      communityAPIBookingService.book(referral, existingAppointment, now.plusDays(1), 60, SERVICE_DELIVERY, "NPS_CODE", null, null)

      verifyNoMoreInteractions(communityAPIClient)
    }

    @Nested
    inner class AppointmentLocationRescheduling {
      @Test
      fun `requests relocation of an appointment when locations are different`() {
        val now = now()
        val deliusAppointmentId = 999L
        val existingAppointment = makeAppointment(now, now, 60, deliusAppointmentId)
        val appointmentDelivery = appointmentDeliveryFactory.create(existingAppointment.id, npsOfficeCode = "OLD_CODE", appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE)
        existingAppointment.appointmentDelivery = appointmentDelivery

        val referral = existingAppointment.referral
        val uri = "/probation-case/${referral.serviceUserCRN}/referrals/${referral.id}/appointments"
        whenever(ramDeliusAPIClient.makePutAppointmentRequest(eq(uri), any())).thenReturn(AppointmentResponseDTO(1234L))

        val res = communityAPIBookingService.book(referral, existingAppointment, now, 60, SERVICE_DELIVERY, "NEW_CODE", null, null)

        val captured = argumentCaptor<AppointmentMerge>()
        verify(ramDeliusAPIClient).makePutAppointmentRequest(eq(uri), captured.capture())

        assertThat(res.first).isEqualTo(1234L)

        val appointmentMerge = captured.firstValue
        assertThat(appointmentMerge.serviceUserCrn).isEqualTo(referral.serviceUserCRN)
        assertThat(appointmentMerge.referralId).isEqualTo(referral.id)
        assertThat(appointmentMerge.officeLocationCode).isEqualTo("NEW_CODE")
      }

      @Test
      fun `requests relocation of an appointment when location is not set on existing appointment `() {
        val now = now()
        val deliusAppointmentId = 999L
        val existingAppointment = makeAppointment(now, now, 60, deliusAppointmentId)
        val appointmentDelivery = appointmentDeliveryFactory.create(existingAppointment.id, appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL)
        existingAppointment.appointmentDelivery = appointmentDelivery

        val referral = existingAppointment.referral
        val uri = "/probation-case/${referral.serviceUserCRN}/referrals/${referral.id}/appointments"
        whenever(ramDeliusAPIClient.makePutAppointmentRequest(eq(uri), any())).thenReturn(AppointmentResponseDTO(1234L))

        val res = communityAPIBookingService.book(referral, existingAppointment, now, 60, SERVICE_DELIVERY, "NEW_CODE", null, null)

        val captured = argumentCaptor<AppointmentMerge>()
        verify(ramDeliusAPIClient).makePutAppointmentRequest(eq(uri), captured.capture())

        assertThat(res.first).isEqualTo(1234L)

        val appointmentMerge = captured.firstValue
        assertThat(appointmentMerge.serviceUserCrn).isEqualTo(referral.serviceUserCRN)
        assertThat(appointmentMerge.referralId).isEqualTo(referral.id)
        assertThat(appointmentMerge.officeLocationCode).isEqualTo("NEW_CODE")
      }

      @Test
      fun `requests relocation of an appointment when location is not set on new appointment `() {
        val now = now()
        val deliusAppointmentId = 999L
        val existingAppointment = makeAppointment(now, now, 60, deliusAppointmentId)
        val appointmentDelivery = appointmentDeliveryFactory.create(existingAppointment.id, npsOfficeCode = "OLD_CODE", appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE)
        existingAppointment.appointmentDelivery = appointmentDelivery

        val referral = existingAppointment.referral
        val uri = "/probation-case/${referral.serviceUserCRN}/referrals/${referral.id}/appointments"
        whenever(ramDeliusAPIClient.makePutAppointmentRequest(eq(uri), any())).thenReturn(AppointmentResponseDTO(1234L))

        communityAPIBookingService.book(referral, existingAppointment, now, 60, SERVICE_DELIVERY, crsOfficeLocation, null, null)

        val captured = argumentCaptor<AppointmentMerge>()
        verify(ramDeliusAPIClient).makePutAppointmentRequest(eq(uri), captured.capture())

        val appointmentMerge = captured.firstValue
        assertThat(appointmentMerge.serviceUserCrn).isEqualTo(referral.serviceUserCRN)
        assertThat(appointmentMerge.referralId).isEqualTo(referral.id)
        assertThat(appointmentMerge.officeLocationCode).isEqualTo(crsOfficeLocation)
      }

      @Test
      fun `does not relocate an appointment when no location is provided and also the existing appointment does not have location set`() {
        val now = now()
        val deliusAppointmentId = 999L
        val existingAppointment = makeAppointment(now, now, 60, deliusAppointmentId)
        val appointmentDelivery = appointmentDeliveryFactory.create(existingAppointment.id, appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL)
        existingAppointment.appointmentDelivery = appointmentDelivery

        communityAPIBookingService.book(referral, existingAppointment, now, 60, SERVICE_DELIVERY, null, null, null)

        verifyNoMoreInteractions(communityAPIClient)
      }
    }
  }

  @Test
  fun `does nothing if not enabled`() {
    val appointment = makeAppointment(now(), now(), 60)

    val communityAPIBookingServiceNotEnabled = CommunityAPIBookingService(
      false,
      httpBaseUrl,
      progressUrl,
      supplierAssessmentUrl,
      rescheduleApiUrl,
      crsOfficeLocation,
      mapOf(),
      mapOf(),
      crsBookingsContext,
      communityAPIClient,
      appointmentMergeUrl,
      ramDeliusAPIClient,
    )

    val (deliusAppointmentId, _) =
      communityAPIBookingServiceNotEnabled.book(referral, appointment, now(), 60, SERVICE_DELIVERY, null, null, null)

    assertThat(deliusAppointmentId).isNull()
    verifyNoInteractions(communityAPIClient)
  }

  @Test
  fun `does nothing if not enabled and returns supplied id for existing delius appointment`() {
    val appointment = makeAppointment(now(), now(), 60, 1234L)

    val communityAPIBookingServiceNotEnabled = CommunityAPIBookingService(
      false,
      httpBaseUrl,
      progressUrl,
      supplierAssessmentUrl,
      rescheduleApiUrl,
      crsOfficeLocation,
      mapOf(),
      mapOf(),
      crsBookingsContext,
      communityAPIClient,
      appointmentMergeUrl,
      ramDeliusAPIClient,
    )

    val (deliusAppointmentId, _) =
      communityAPIBookingServiceNotEnabled.book(referral, appointment, now(), 60, SERVICE_DELIVERY, null, null, null)

    assertThat(deliusAppointmentId).isEqualTo(1234L)
    verifyNoInteractions(communityAPIClient)
  }

  private fun makeAppointment(
    createdAt: OffsetDateTime,
    appointmentTime: OffsetDateTime,
    durationInMinutes: Int,
    deliusAppointmentId: Long? = null,
  ): Appointment {
    return appointmentFactory.create(
      createdAt = createdAt,
      appointmentTime = appointmentTime,
      durationInMinutes = durationInMinutes,
      deliusAppointmentId = deliusAppointmentId,
    )
  }
}
