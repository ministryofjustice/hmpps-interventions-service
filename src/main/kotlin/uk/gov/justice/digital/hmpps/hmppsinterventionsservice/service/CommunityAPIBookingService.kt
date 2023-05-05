package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.RamDeliusClient
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended.NO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.NotNull

@Service
@Transactional
class CommunityAPIBookingService(
  @Value("\${community-api.appointments.bookings.enabled}") private val bookingsEnabled: Boolean,
  @Value("\${interventions-ui.baseurl}") private val interventionsUIBaseURL: String,
  @Value("\${interventions-ui.locations.probation-practitioner.intervention-progress}") private val ppInterventionProgressLocation: String,
  @Value("\${interventions-ui.locations.probation-practitioner.supplier-assessment}") private val ppSupplierAssessmentLocation: String,
  @Value("\${community-api.appointments.office-location}") private val defaultOfficeLocation: String,
  @Value("#{\${community-api.appointments.notes-field-qualifier}}") private val notesFieldQualifier: Map<AppointmentType, String>,
  @Value("#{\${community-api.appointments.counts-towards-rar-days}}") private val countsTowardsRarDays: Map<AppointmentType, Boolean>,
  @Value("\${refer-and-monitor-and-delius.locations.appointment-merge}") private val appointmentMergeLocation: String,
  private val ramDeliusClient: RamDeliusClient,
) : CommunityAPIService {
  companion object : KLogging()

  fun book(
    referral: Referral,
    existingAppointment: Appointment?,
    appointmentTime: OffsetDateTime,
    durationInMinutes: Int,
    appointmentType: AppointmentType,
    npsOfficeCode: String?,
    attended: Attended? = null,
    notifyPPOfAttendanceBehaviour: Boolean? = null,
  ): Pair<Long?, UUID?> {
    if (!bookingsEnabled) {
      return existingAppointment?.deliusAppointmentId to null
    }

    return processingBooking(referral, existingAppointment, appointmentTime, durationInMinutes, appointmentType, npsOfficeCode, attended, notifyPPOfAttendanceBehaviour)
  }

  private fun processingBooking(
    referral: Referral,
    existingAppointment: Appointment?,
    appointmentTime: OffsetDateTime,
    durationInMinutes: Int,
    appointmentType: AppointmentType,
    npsOfficeCode: String?,
    attended: Attended?,
    notifyPPOfAttendanceBehaviour: Boolean?,
  ): Pair<Long?, UUID> {
    return existingAppointment?.let {
      if (isDifferentTimings(existingAppointment, appointmentTime, durationInMinutes) ||
        isDifferentLocation(existingAppointment, npsOfficeCode)
      ) {
        val appointmentMerge = existingAppointment.forMerge(
          appointmentType,
          npsOfficeCode ?: defaultOfficeLocation,
          notifyPPOfAttendanceBehaviour ?: false,
        )
        mergeAppointment(appointmentMerge)
      } else {
        // nothing to do !
        return existingAppointment.deliusAppointmentId to existingAppointment.id
      }
    } ?: run {
      val mergeAppointment = AppointmentMerge(
        UUID.randomUUID(),
        referral.id,
        referral.referenceNumber!!,
        referral.serviceUserCRN,
        appointmentTime,
        appointmentTime.plusMinutes(durationInMinutes.toLong()),
        getNotes(
          referral,
          buildReferralResourceUrl(referral, appointmentType),
          "${get(appointmentType, notesFieldQualifier)} Appointment",
        ),
        npsOfficeCode ?: defaultOfficeLocation,
        get(appointmentType, countsTowardsRarDays),
        attended?.let { AppointmentMerge.Outcome(it, (attended == NO || notifyPPOfAttendanceBehaviour == true)) },
        null,
        null,
      )
      mergeAppointment(mergeAppointment)
    }
  }

  private fun Appointment.forMerge(
    appointmentType: AppointmentType,
    npsOfficeCode: String,
    notifyOfAttendanceBehaviour: Boolean,
  ) = AppointmentMerge(
    UUID.randomUUID(),
    referral.id,
    referral.referenceNumber!!,
    referral.serviceUserCRN,
    appointmentTime,
    appointmentTime.plusMinutes(durationInMinutes.toLong()),
    getNotes(
      referral,
      buildReferralResourceUrl(referral, appointmentType),
      "${get(appointmentType, notesFieldQualifier)} Appointment",
    ),
    npsOfficeCode,
    get(appointmentType, countsTowardsRarDays),
    attended?.let { AppointmentMerge.Outcome(it, attended == NO || notifyOfAttendanceBehaviour) },
    id,
    deliusAppointmentId,
  )

  private fun mergeAppointment(appointmentMerge: AppointmentMerge): Pair<Long?, UUID> {
    val path = UriComponentsBuilder.fromPath(appointmentMergeLocation)
      .buildAndExpand(appointmentMerge.serviceUserCrn, appointmentMerge.referralId)
      .toString()
    return ramDeliusClient.makePutAppointmentRequest(path, appointmentMerge)?.appointmentId to appointmentMerge.id
  }

  fun setNotifyPPIfAttendedNo(attended: Attended?, notifyPPOfAttendanceBehaviour: Boolean?) =
    attended == NO || notifyPPOfAttendanceBehaviour == true

  private fun buildReferralResourceUrl(referral: Referral, appointmentType: AppointmentType): String {
    val location = when (appointmentType) {
      AppointmentType.SERVICE_DELIVERY -> ppInterventionProgressLocation
      AppointmentType.SUPPLIER_ASSESSMENT -> ppSupplierAssessmentLocation
    }

    return UriComponentsBuilder.fromHttpUrl(interventionsUIBaseURL)
      .path(location)
      .buildAndExpand(referral.id)
      .toString()
  }

  fun isRescheduleBooking(existingAppointment: Appointment, appointmentTime: OffsetDateTime, durationInMinutes: Int, npsOfficeCode: String?): Boolean =
    isDifferentTimings(existingAppointment, appointmentTime, durationInMinutes)

  private fun isDifferentLocation(existingAppointment: Appointment, npsOfficeCode: String?): Boolean {
    return !npsOfficeCode.equals(existingAppointment.appointmentDelivery?.npsOfficeCode)
  }
  private fun isDifferentTimings(existingAppointment: Appointment, appointmentTime: OffsetDateTime, durationInMinutes: Int): Boolean =
    !existingAppointment.appointmentTime.isEqual(appointmentTime) || existingAppointment.durationInMinutes != durationInMinutes

  private inline fun <reified T> get(appointmentType: AppointmentType, map: Map<AppointmentType, T>): T {
    return map[appointmentType] ?: throw IllegalStateException("Property value not found for $appointmentType")
  }
}

data class AppointmentMerge(
  val id: UUID,
  val referralId: UUID,
  val referralReference: String,
  val serviceUserCrn: String,
  val start: OffsetDateTime,
  val end: OffsetDateTime,
  val notes: String?,
  val officeLocationCode: String?,
  val countsTowardsRar: Boolean,
  val outcome: Outcome?,
  val previousId: UUID?,
  val deliusId: Long?,
) {
  data class Outcome(
    val attended: Attended,
    val notify: Boolean = true,
  )
}

data class AppointmentResponseDTO(
  @NotNull val appointmentId: Long,
)
