package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentDeliveryType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentSessionType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType.SUPPLIER_ASSESSMENT
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SupplierAssessment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.SupplierAssessmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.OffsetDateTime
import java.util.Optional.empty
import java.util.UUID

class SupplierAssessmentServiceTest {
  private val supplierAssessmentRepository: SupplierAssessmentRepository = mock()
  private val referralRepository: ReferralRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentService: AppointmentService = mock()

  private val authUserFactory = AuthUserFactory()
  private val referralFactory = ReferralFactory()
  private val appointmentFactory = AppointmentFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()

  private val supplierAssessmentService = SupplierAssessmentService(
    supplierAssessmentRepository,
    referralRepository,
    appointmentService,
  )

  @Test
  fun `supplier assessment can be created`() {
    val referral = referralFactory.createSent()

    whenever(appointmentRepository.save(any())).thenReturn(appointmentFactory.create())
    whenever(supplierAssessmentRepository.save(any())).thenReturn(supplierAssessmentFactory.create())
    whenever(referralRepository.save(any())).thenReturn(referral)

    val response = supplierAssessmentService.createSupplierAssessment(referral)

    val argumentCaptor = argumentCaptor<SupplierAssessment>()
    verify(supplierAssessmentRepository, atLeastOnce()).save(argumentCaptor.capture())
    val arguments = argumentCaptor.firstValue

    assertThat(arguments.referral).isEqualTo(referral)
    assertThat(response.supplierAssessment).isNotNull
  }

  @Nested
  inner class CreateOrUpdateSupplierAssessmentAppointment {
    val supplierAssessment = supplierAssessmentFactory.createWithNoAppointment()
    val appointment = appointmentFactory.create()
    val createdByUser = authUserFactory.create()
    val durationInMinutes = 60
    val appointmentTime = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    var appointmentDeliveryType = AppointmentDeliveryType.PHONE_CALL
    var appointmentSessionType = AppointmentSessionType.ONE_TO_ONE

    @Test
    fun `can create supplier assessment appointment`() {
      whenever(
        appointmentService.createOrUpdateAppointment(
          eq(supplierAssessment.referral),
          isNull(),
          eq(durationInMinutes),
          eq(appointmentTime),
          eq(SUPPLIER_ASSESSMENT),
          eq(createdByUser),
          eq(appointmentDeliveryType),
          eq(appointmentSessionType),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
        ),
      ).thenReturn(appointment)
      whenever(supplierAssessmentRepository.save(any())).thenReturn(supplierAssessment)

      supplierAssessmentService.createOrUpdateSupplierAssessmentAppointment(supplierAssessment, durationInMinutes, appointmentTime, createdByUser, appointmentDeliveryType, appointmentSessionType)

      val argumentCaptor = argumentCaptor<SupplierAssessment>()
      verify(supplierAssessmentRepository, atLeastOnce()).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.appointments.size).isEqualTo(1)
      assertThat(arguments.currentAppointment).isEqualTo(appointment)
    }

    @Test
    fun `can create supplier assessment appointment with delius office location`() {
      appointmentDeliveryType = AppointmentDeliveryType.IN_PERSON_MEETING_PROBATION_OFFICE
      val npsOfficeCode = "CRSEXT"

      whenever(
        appointmentService.createOrUpdateAppointment(
          eq(supplierAssessment.referral),
          isNull(),
          eq(durationInMinutes),
          eq(appointmentTime),
          eq(SUPPLIER_ASSESSMENT),
          eq(createdByUser),
          eq(appointmentDeliveryType),
          eq(appointmentSessionType),
          isNull(),
          eq(npsOfficeCode),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
        ),
      ).thenReturn(appointment)
      whenever(supplierAssessmentRepository.save(any())).thenReturn(supplierAssessment)

      supplierAssessmentService.createOrUpdateSupplierAssessmentAppointment(supplierAssessment, durationInMinutes, appointmentTime, createdByUser, appointmentDeliveryType, appointmentSessionType, npsOfficeCode = npsOfficeCode)

      val argumentCaptor = argumentCaptor<SupplierAssessment>()
      verify(supplierAssessmentRepository, atLeastOnce()).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.appointments.size).isEqualTo(1)
      assertThat(arguments.currentAppointment).isEqualTo(appointment)
    }

    @Test
    fun `can replace the existing appointment with the new appointment`() {
      val appointment = appointmentFactory.create()
      val supplierAssessment = supplierAssessmentFactory.create(appointment = appointment)
      val newAppointment = appointmentFactory.create()

      whenever(
        appointmentService.createOrUpdateAppointment(
          eq(supplierAssessment.referral),
          eq(supplierAssessment.currentAppointment),
          eq(durationInMinutes),
          eq(appointmentTime),
          eq(SUPPLIER_ASSESSMENT),
          eq(createdByUser),
          eq(appointmentDeliveryType),
          eq(appointmentSessionType),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
        ),
      ).thenReturn(newAppointment)
      whenever(supplierAssessmentRepository.save(any())).thenReturn(supplierAssessment)

      val supplierAssessmentAppointment = supplierAssessmentService.createOrUpdateSupplierAssessmentAppointment(supplierAssessment, durationInMinutes, appointmentTime, createdByUser, appointmentDeliveryType, appointmentSessionType)

      val argumentCaptor = argumentCaptor<SupplierAssessment>()
      verify(supplierAssessmentRepository, atLeastOnce()).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.appointments.size).isEqualTo(2)
      assertThat(arguments.currentAppointment).isEqualTo(supplierAssessmentAppointment)
    }

    @Test
    fun `can create a new appointment when the previous appointment was not attended`() {
      val appointment = appointmentFactory.create(attended = Attended.NO)
      val supplierAssessment = supplierAssessmentFactory.create(appointment = appointment)
      val newAppointment = appointmentFactory.create()
      val attended = Attended.NO

      whenever(
        appointmentService.createOrUpdateAppointment(
          eq(supplierAssessment.referral),
          eq(supplierAssessment.currentAppointment),
          eq(durationInMinutes),
          eq(appointmentTime),
          eq(SUPPLIER_ASSESSMENT),
          eq(createdByUser),
          eq(appointmentDeliveryType),
          eq(appointmentSessionType),
          isNull(),
          isNull(),
          eq(attended),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
        ),
      ).thenReturn(newAppointment)
      whenever(supplierAssessmentRepository.save(any())).thenReturn(supplierAssessment)

      val supplierAssessmentAppointment = supplierAssessmentService.createOrUpdateSupplierAssessmentAppointment(supplierAssessment, durationInMinutes, appointmentTime, createdByUser, appointmentDeliveryType, appointmentSessionType, attended = attended)

      val argumentCaptor = argumentCaptor<SupplierAssessment>()
      verify(supplierAssessmentRepository, atLeastOnce()).save(argumentCaptor.capture())
      val arguments = argumentCaptor.firstValue

      assertThat(arguments.appointments.size).isEqualTo(2)
      assertThat(arguments.currentAppointment).isEqualTo(supplierAssessmentAppointment)
    }
  }

  @Test
  fun `get supplier assessment throws error`() {
    val supplierAssessmentId = UUID.randomUUID()
    whenever(supplierAssessmentRepository.findById(any())).thenReturn(empty())

    val error = assertThrows<EntityNotFoundException> {
      supplierAssessmentService.getSupplierAssessmentById(supplierAssessmentId)
    }
    assertThat(error.message).contains("Supplier Assessment not found [id=$supplierAssessmentId]")
  }
}
