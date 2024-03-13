package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Attended
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.serviceprovider.performance.PerformanceReportProcessor
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ActionPlanService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.SupplierAssessmentFactory
import java.time.OffsetDateTime

internal class PerformanceReportDataProcessorTest {
  private val actionPlanService = mock<ActionPlanService>()
  private val appointmentFactory = AppointmentFactory()
  private val supplierAssessmentFactory = SupplierAssessmentFactory()
  private val appointmentRepository: AppointmentRepository = mock()
  private val actionPlanFactory = ActionPlanFactory()

  private val processor = PerformanceReportProcessor(actionPlanService)

  private val referralFactory = ReferralFactory()

  @Test
  fun `will process sent referrals`() {
    val supplierAssessmentFirstAppointment = appointmentFactory.create(attended = Attended.NO, createdAt = OffsetDateTime.parse("2022-07-01T10:42:43+00:00"))
    val supplierAssessmentNewAppointment = appointmentFactory.create(attended = Attended.YES, createdAt = OffsetDateTime.parse("2022-07-02T10:42:43+00:00"))
    val approvedActionPlanAppointment = appointmentFactory.create(attended = Attended.YES, appointmentFeedbackSubmittedAt = OffsetDateTime.parse("2022-07-02T10:42:43+00:00"))
    val unApprovedAppointment = appointmentFactory.create()
    val supplierAssessment = supplierAssessmentFactory.create(appointment = supplierAssessmentFirstAppointment)
    supplierAssessment.appointments.add(supplierAssessmentNewAppointment)
    val actionPlan = actionPlanFactory.createApproved()
    val referral = referralFactory.createSent(actionPlans = mutableListOf(actionPlan), supplierAssessment = supplierAssessment)

    whenever(appointmentRepository.findAllByReferralId(referral.id)).thenReturn(listOf(supplierAssessmentFirstAppointment, supplierAssessmentNewAppointment))
    whenever(actionPlanService.getFirstAttendedAppointment(actionPlan)).thenReturn(approvedActionPlanAppointment)
    whenever(actionPlanService.getAllCompletedAppointments(actionPlan)).thenReturn(listOf(approvedActionPlanAppointment))

    val performanceReportData = processor.process(referral)

    assertThat(performanceReportData.referralId).isEqualTo(referral.id)
    assertThat(performanceReportData.dateReferralReceived).isEqualTo(referral.sentAt)
    assertThat(performanceReportData.dateReferralReceived).isEqualTo(referral.sentAt)
    assertThat(performanceReportData.contractReference).isEqualTo(referral.intervention.dynamicFrameworkContract.contractReference)
    assertThat(performanceReportData.dateSupplierAssessmentFirstArranged).isEqualTo(supplierAssessmentFirstAppointment.createdAt)
    assertThat(performanceReportData.dateSupplierAssessmentFirstNotAttended).isEqualTo(supplierAssessmentFirstAppointment.appointmentTime)
    assertThat(performanceReportData.dateSupplierAssessmentFirstScheduledFor).isEqualTo(supplierAssessmentFirstAppointment.appointmentTime)
    assertThat(performanceReportData.dateSupplierAssessmentFirstAttended).isEqualTo(supplierAssessmentNewAppointment.appointmentTime)
    assertThat(performanceReportData.firstSessionAttendedAt).isEqualTo(approvedActionPlanAppointment.appointmentTime)
    assertThat(performanceReportData.numberOfSessionsAttended).isEqualTo(1)
    assertThat(performanceReportData.supplierAssessmentAttendedOnTime).isEqualTo(true)
  }
}
