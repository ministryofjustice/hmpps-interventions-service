package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralPerformanceReportRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.PerformanceReportProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.model.ReferralPerformanceReport
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

internal class PerformanceReportDataProcessorTest {
  private val actionPlanService = mock<ActionPlanService>()
  private val appointmentFactory = AppointmentFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()
  private val appointmentRepository: AppointmentRepository = mock()
  private val referralPerformanceReportRepository: ReferralPerformanceReportRepository = mock()
  private val actionPlanFactory = ActionPlanFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val processor = PerformanceReportProcessor(referralPerformanceReportRepository)

  private val referralFactory = ReferralFactory()

  @Test
  fun `will process sent referrals`() {
    val supplierAssessmentFirstAppointment = appointmentFactory.create(attended = Attended.NO, createdAt = OffsetDateTime.parse("2022-07-01T10:42:43+00:00"))
    val supplierAssessmentNewAppointment = appointmentFactory.create(attended = Attended.YES, createdAt = OffsetDateTime.parse("2022-07-02T10:42:43+00:00"))
    val approvedActionPlanAppointment = appointmentFactory.create(attended = Attended.YES, appointmentFeedbackSubmittedAt = OffsetDateTime.parse("2022-07-02T10:42:43+00:00"))
    val supplierAssessment = supplierAssessmentFactory.create(appointment = supplierAssessmentFirstAppointment)
    supplierAssessment.appointments.add(supplierAssessmentNewAppointment)
    val actionPlan = actionPlanFactory.createApproved()
    val endOfServiceReport = endOfServiceReportFactory.create()
    val referral = referralFactory.createSent(actionPlans = mutableListOf(actionPlan), supplierAssessment = supplierAssessment)

    val today = OffsetDateTime.now().toLocalDate()

    val referralPerformanceReport = ReferralPerformanceReport(
      referralReference = referral.referenceNumber!!,
      referralId = referral.id,
      contractReference = referral.intervention.dynamicFrameworkContract.contractReference,
      organisationId = referral.intervention.dynamicFrameworkContract.primeProvider.id,
      currentAssigneeEmail = "a.b@xyz.com",
      crn = referral.serviceUserCRN,
      dateReferralReceived = referral.sentAt!!,
      dateSupplierAssessmentFirstArranged = supplierAssessmentFirstAppointment.createdAt,
      dateSupplierAssessmentFirstScheduledFor = supplierAssessmentFirstAppointment.appointmentTime,
      dateSupplierAssessmentFirstNotAttended = supplierAssessmentFirstAppointment.appointmentTime,
      dateSupplierAssessmentFirstAttended = supplierAssessmentNewAppointment.appointmentTime,
      dateSupplierAssessmentFirstCompleted = supplierAssessmentNewAppointment.appointmentTime,
      supplierAssessmentAttendedOnTime = true,
      firstActionPlanSubmittedAt = actionPlan.submittedAt,
      firstActionPlanApprovedAt = actionPlan.approvedAt,
      approvedActionPlanId = actionPlan.id,
      numberOfOutcomes = 2,
      endOfServiceReportId = null,
      numberOfSessions = 2,
      endRequestedAt = OffsetDateTime.now(),
      endRequestedReason = "reason",
      eosrSubmittedAt = OffsetDateTime.now(),
      concludedAt = referral.concludedAt,
      completionDeadline = today,
      endOfSentenceDate = today.plusDays(60),
    )

    whenever(appointmentRepository.findAllByReferralId(referral.id)).thenReturn(listOf(supplierAssessmentFirstAppointment, supplierAssessmentNewAppointment))
    whenever(referralPerformanceReportRepository.eosrAchievementScore(anyOrNull())).thenReturn(BigDecimal.valueOf(1L))
    whenever(referralPerformanceReportRepository.firstAttendanceDate(anyOrNull())).thenReturn(approvedActionPlanAppointment.appointmentTime)
    whenever(referralPerformanceReportRepository.attendanceCount(anyOrNull())).thenReturn(Integer(2))
    whenever(actionPlanService.getFirstAttendedAppointment(referral.id)).thenReturn(approvedActionPlanAppointment)

    val performanceReportData = processor.process(referralPerformanceReport)

    assertThat(performanceReportData.referralId).isEqualTo(referral.id)
    assertThat(performanceReportData.dateReferralReceived).isEqualTo(referral.sentAt)
    assertThat(performanceReportData.dateReferralReceived).isEqualTo(referral.sentAt)
    assertThat(performanceReportData.contractReference).isEqualTo(referral.intervention.dynamicFrameworkContract.contractReference)
    assertThat(performanceReportData.dateSupplierAssessmentFirstArranged).isEqualTo(supplierAssessmentFirstAppointment.createdAt)
    assertThat(performanceReportData.dateSupplierAssessmentFirstNotAttended).isEqualTo(supplierAssessmentFirstAppointment.appointmentTime)
    assertThat(performanceReportData.dateSupplierAssessmentFirstScheduledFor).isEqualTo(supplierAssessmentFirstAppointment.appointmentTime)
    assertThat(performanceReportData.dateSupplierAssessmentFirstAttended).isEqualTo(supplierAssessmentNewAppointment.appointmentTime)
    assertThat(performanceReportData.firstSessionAttendedAt).isEqualTo(approvedActionPlanAppointment.appointmentTime)
    assertThat(performanceReportData.numberOfSessionsAttended).isEqualTo(2)
    assertThat(performanceReportData.supplierAssessmentAttendedOnTime).isEqualTo(true)
    assertThat(performanceReportData.dateInterventionToBeCompletedBy).isEqualTo(today)
    assertThat(performanceReportData.endOfSentenceDate).isEqualTo(today.plusDays(60))
  }

  @Test
  fun `will process sent referrals where numberOfOutcomes is null`() {
    val supplierAssessmentFirstAppointment = appointmentFactory.create(attended = Attended.NO, createdAt = OffsetDateTime.parse("2022-07-01T10:42:43+00:00"))
    val supplierAssessmentNewAppointment = appointmentFactory.create(attended = Attended.YES, createdAt = OffsetDateTime.parse("2022-07-02T10:42:43+00:00"))
    val approvedActionPlanAppointment = appointmentFactory.create(attended = Attended.YES, appointmentFeedbackSubmittedAt = OffsetDateTime.parse("2022-07-02T10:42:43+00:00"))
    val supplierAssessment = supplierAssessmentFactory.create(appointment = supplierAssessmentFirstAppointment)
    supplierAssessment.appointments.add(supplierAssessmentNewAppointment)
    val actionPlan = actionPlanFactory.createApproved()
    val referral = referralFactory.createSent(actionPlans = mutableListOf(actionPlan), supplierAssessment = supplierAssessment)

    val today = OffsetDateTime.now().toLocalDate()

    val referralPerformanceReport = ReferralPerformanceReport(
      referralReference = referral.referenceNumber!!,
      referralId = referral.id,
      contractReference = referral.intervention.dynamicFrameworkContract.contractReference,
      organisationId = referral.intervention.dynamicFrameworkContract.primeProvider.id,
      currentAssigneeEmail = "a.b@xyz.com",
      crn = referral.serviceUserCRN,
      dateReferralReceived = referral.sentAt!!,
      dateSupplierAssessmentFirstArranged = supplierAssessmentFirstAppointment.createdAt,
      dateSupplierAssessmentFirstScheduledFor = supplierAssessmentFirstAppointment.appointmentTime,
      dateSupplierAssessmentFirstNotAttended = supplierAssessmentFirstAppointment.appointmentTime,
      dateSupplierAssessmentFirstAttended = supplierAssessmentNewAppointment.appointmentTime,
      dateSupplierAssessmentFirstCompleted = supplierAssessmentNewAppointment.appointmentTime,
      supplierAssessmentAttendedOnTime = true,
      firstActionPlanSubmittedAt = actionPlan.submittedAt,
      firstActionPlanApprovedAt = actionPlan.approvedAt,
      approvedActionPlanId = actionPlan.id,
      numberOfOutcomes = null,
      endOfServiceReportId = null,
      numberOfSessions = 2,
      endRequestedAt = OffsetDateTime.now(),
      endRequestedReason = "reason",
      eosrSubmittedAt = OffsetDateTime.now(),
      concludedAt = referral.concludedAt,
      completionDeadline = today,
      endOfSentenceDate = today.plusDays(60),
    )

    whenever(appointmentRepository.findAllByReferralId(referral.id)).thenReturn(listOf(supplierAssessmentFirstAppointment, supplierAssessmentNewAppointment))
    whenever(referralPerformanceReportRepository.eosrAchievementScore(anyOrNull())).thenReturn(BigDecimal.valueOf(1L))
    whenever(referralPerformanceReportRepository.firstAttendanceDate(anyOrNull())).thenReturn(approvedActionPlanAppointment.appointmentTime)
    whenever(referralPerformanceReportRepository.attendanceCount(anyOrNull())).thenReturn(Integer(2))
    whenever(actionPlanService.getFirstAttendedAppointment(referral.id)).thenReturn(approvedActionPlanAppointment)

    val performanceReportData = processor.process(referralPerformanceReport)

    assertThat(performanceReportData.numberOfOutcomes).isNull()
  }
}
