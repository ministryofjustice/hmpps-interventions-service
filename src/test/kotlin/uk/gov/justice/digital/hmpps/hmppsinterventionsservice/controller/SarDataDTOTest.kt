package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SarDataDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AchievementLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanActivity
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.EndOfServiceReportOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.SarsReferralData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ActionPlanFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.EndOfServiceReportFactory
import java.time.OffsetDateTime
import java.util.UUID

@JsonTest
class SarDataDTOTest(@Autowired private val json: JacksonTester<SarDataDTO>) {
  private val deliverySessionFactory = DeliverySessionFactory()
  private val actionPlanFactory = ActionPlanFactory()
  private val endOfServiceReportFactory = EndOfServiceReportFactory()

  @Test
  fun `return sars data in the request format`() {
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
            desiredOutcome = DesiredOutcome(id = UUID.randomUUID(), description = "desired outcome", serviceCategoryId = UUID.randomUUID(), deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf()),
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

    val sarsReferralData = SarsReferralData(referral, session1.appointments.toList() + session2.appointments.toList())

    val out = json.write(SarDataDTO.from("X123456", listOf(sarsReferralData)))

    assertThat(out).isEqualToJson(
      """
          {
            "content": {
              "crn": "X123456",
              "referral": [
                {
                  "referral_number": "something",
                  "accessibility_needs": "wheelchair",
                  "additional_needs_information": "english",
                  "when_unavailable": "tomorrow",
                  "end_requested_comments": "appointment needed",
                  "appointment": [
                    {
                      "session_summary": "PP attended",
                      "session_response": "It went well",
                      "session_concerns": "some concern",
                      "late_reason": "Have to go the hosiptal",
                      "future_session_plan": "have to invite him again"
                    },
                    {
                      "session_summary": "PP did not attended",
                      "session_response": "It didn't go well",
                      "session_concerns": "some other concern",
                      "late_reason": "Have been drinking",
                      "future_session_plan": "have to invite him again"
                    }
                  ],
                  "action_plan_activity": [
                    {
                      "description": ["action plan approved"]
                    }
                  ],
                "end_of_service_report": {
                  "end_of_service_outcomes": [
                    {
                      "progression_comments": "progressing level",
                      "additional_task_comments": "he needs interview"
                    }
                  ]
                }
                }
              ]
            }
          }
      """.trimIndent(),
    )
  }

  @Test
  fun `return sars data when there is no appointment`() {
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
            desiredOutcome = DesiredOutcome(id = UUID.randomUUID(), description = "desired outcome", serviceCategoryId = UUID.randomUUID(), deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf()),
          ),
        ),
      ),
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

    val sarsReferralData = SarsReferralData(referral, emptyList())

    val out = json.write(SarDataDTO.from("X123456", listOf(sarsReferralData)))

    assertThat(out).isEqualToJson(
      """
        {
          "content": {
            "crn": "X123456",
            "referral": [
              {
                "referral_number": "something",
                "accessibility_needs": "wheelchair",
                "additional_needs_information": "english",
                "when_unavailable": "tomorrow",
                "end_requested_comments": "appointment needed",
                "appointment": [],
                "action_plan_activity": [
                  {
                    "description": [
                      "action plan approved"
                    ]
                  }
                ],
                "end_of_service_report": {
                  "end_of_service_outcomes": [
                    {
                      "progression_comments": "progressing level",
                      "additional_task_comments": "he needs interview"
                    }
                  ]
                }
              }
            ]
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `return sars data when there is no referral associated with the crn`() {
    val out = json.write(SarDataDTO.from("X123456", emptyList()))

    assertThat(out).isEqualToJson(
      """
            {
              "content": {
                "crn": "X123456",
                "referral": []
              }
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `return sars data when some of the fields are not present`() {
    val id = UUID.randomUUID()
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")

    val referral = SampleData.sampleReferral(
      "X123456",
      "Provider",
      id = id,
      createdAt = createdAt,
      referenceNumber = "1234",
      accessibilityNeeds = null,
      additionalNeedsInformation = null,
      whenUnavailable = null,
      endRequestedComments = null,
      endOfServiceReport = endOfServiceReportFactory.create(
        outcomes = mutableSetOf(
          EndOfServiceReportOutcome(
            progressionComments = null,
            additionalTaskComments = null,
            achievementLevel = AchievementLevel.ACHIEVED,
            desiredOutcome = DesiredOutcome(id = UUID.randomUUID(), description = "desired outcome", serviceCategoryId = UUID.randomUUID(), deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf()),
          ),
        ),
      ),
    )

    val session1 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 1,
      sessionConcerns = null,
      sessionSummary = "PP attended",
      sessionResponse = "It went well",
      lateReason = null,
      futureSessionPlan = "have to invite him again",
    )
    val session2 = deliverySessionFactory.createAttended(
      referral = referral,
      sessionNumber = 2,
      sessionConcerns = "some other concern",
      sessionSummary = null,
      sessionResponse = null,
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

    val sarsReferralData = SarsReferralData(referral, session1.appointments.toList() + session2.appointments.toList())

    val out = json.write(SarDataDTO.from("X123456", listOf(sarsReferralData)))

    assertThat(out).isEqualToJson(
      """
            {
              "content": {
                "crn": "X123456",
                "referral": [
                  {
                    "referral_number": "something",
                    "accessibility_needs": "",
                    "additional_needs_information": "",
                    "when_unavailable": "",
                    "end_requested_comments": "",
                    "appointment": [
                      {
                        "session_summary": "PP attended",
                        "session_response": "It went well",
                        "session_concerns": "",
                        "late_reason": "",
                        "future_session_plan": "have to invite him again"
                      },
                      {
                        "session_summary": "",
                        "session_response": "",
                        "session_concerns": "some other concern",
                        "late_reason": "Have been drinking",
                        "future_session_plan": "have to invite him again"
                      }
                    ],
                    "action_plan_activity": [
                      {
                        "description": ["action plan approved"]
                      }
                    ],
                  "end_of_service_report": {
                    "end_of_service_outcomes":[
                    {
                      "progression_comments": "",
                      "additional_task_comments": ""
                     }
                    ]
                  }
                  }
                ]
              }
            }
      """.trimIndent(),
    )
  }
}
