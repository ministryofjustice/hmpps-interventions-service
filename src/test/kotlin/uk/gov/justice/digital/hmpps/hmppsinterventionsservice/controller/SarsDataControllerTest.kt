package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatusCode
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AchievementLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanActivity
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReportOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SarsDataService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SarsReferralData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import java.time.OffsetDateTime
import java.util.UUID

internal class SarsDataControllerTest {

  private val deliverySessionFactory = DeliverySessionFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  private val sarsDataService: SarsDataService = mock()
  private val sarsDataController = SarsDataController(sarsDataService)

  @Test
  fun `retrieve referral sars data`() {
    val jwtAuthenticationToken = JwtAuthenticationToken(mock())

    val id = UUID.randomUUID()
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")

    val referral = SampleData.sampleReferral(
      "X123456",
      "Provider",
      id = id,
      createdAt = createdAt,
      referenceNumber = "1234",
      accessibilityNeeds = "wheelchair",
      additionalNeedsInformation = "english",
      whenUnavailable = "tomorrow",
      endRequestedComments = "appointment needed",
      endOfServiceReport = endOfServiceReportFactory.create(
        outcomes = mutableSetOf(
          EndOfServiceReportOutcome(
            progressionComments = "progressing level",
            additionalTaskComments = "he needs interview",
            achievementLevel = AchievementLevel.ACHIEVED,
            desiredOutcome = DesiredOutcome(id = UUID.randomUUID(), description = "desired outcome", serviceCategoryId = UUID.randomUUID()),
          ),
        ),
      ),
    )

    val session1 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 1,
      sessionConcerns = "some concern",
      sessionSummary = "PP attended",
      sessionResponse = "It went well",
      lateReason = "Have to go the hosiptal",
      futureSessionPlan = "have to invite him again",
    )
    val session2 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 2,
      sessionConcerns = "some other concern",
      sessionSummary = "PP did not attended",
      sessionResponse = "It didn't go well",
      lateReason = "Have been drinking",
      futureSessionPlan = "have to invite him again",
    )

    val actionPlan = actionPlanFactory.createApproved(
      referral = referral,
      activities = mutableListOf(ActionPlanActivity(description = "action plan approved")),
    )

    referral.referenceNumber = "something"
    referral.needsInterpreter = true
    referral.interpreterLanguage = "french"
    referral.supplementaryRiskId = UUID.fromString("1c893c33-a373-435e-b13f-7efbc6206e2a")
    referral.relevantSentenceId = 123456L
    referral.actionPlans = mutableListOf(actionPlan)

    val sarsReferralData = listOf(SarsReferralData(referral, session1.appointments.toList() + session2.appointments.toList()))

    whenever(sarsDataService.getSarsReferralData("crn")).thenReturn(sarsReferralData)

    val sarsData = sarsDataController.sarData(crn = "crn", authentication = jwtAuthenticationToken)

    assertThat(sarsData.statusCode).isEqualTo(HttpStatusCode.valueOf(200))
    assertThat(sarsData.body?.content?.referral?.size).isEqualTo(1)
  }

  @Test
  fun `returns 209 when crn is not passed as parameter`() {
    val jwtAuthenticationToken = JwtAuthenticationToken(mock())

    val sarsData = sarsDataController.sarData(crn = null, authentication = jwtAuthenticationToken)

    assertThat(sarsData.statusCode).isEqualTo(HttpStatusCode.valueOf(209))
    assertThat(sarsData.body).isNull()
  }

  @Test
  fun `returns 204 when no data returned for the crn `() {
    val jwtAuthenticationToken = JwtAuthenticationToken(mock())
    whenever(sarsDataService.getSarsReferralData("crn")).thenReturn(emptyList())

    val sarsData = sarsDataController.sarData("crn", authentication = jwtAuthenticationToken)

    assertThat(sarsData.statusCode).isEqualTo(HttpStatusCode.valueOf(204))
    assertThat(sarsData.body?.content?.crn).isEqualTo("crn")
    assertThat(sarsData.body?.content?.referral).isEmpty()
  }
}
