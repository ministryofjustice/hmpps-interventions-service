package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.SarDataDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AchievementLevel
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ActionPlanActivity
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
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
    val sentAt = createdAt.plusDays(1)
    val endRequestedAt = createdAt.plusDays(2)
    val concludedAt = createdAt.plusDays(3)

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
        createdAt = createdAt,
        submittedAt = sentAt,
        furtherInformation = "further info",
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
    referral.hasAdditionalResponsibilities = true
    referral.sentAt = sentAt
    referral.endRequestedAt = endRequestedAt
    referral.concludedAt = concludedAt
    referral.additionalRiskInformationUpdatedAt = createdAt
    referral.withdrawalComments = "withdrawn"

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
      createdAt = createdAt,
      numberOfSessions = 3,
      activities = mutableListOf(ActionPlanActivity(description = "action plan approved", createdAt = createdAt)),
    )

    referral.referenceNumber = "something"
    referral.needsInterpreter = true
    referral.interpreterLanguage = "french"
    referral.supplementaryRiskId = UUID.fromString("1c893c33-a373-435e-b13f-7efbc6206e2a")
    referral.relevantSentenceId = 123456L
    referral.actionPlans = mutableListOf(actionPlan)

    val caseNotes = listOf(
      SampleData.sampleCaseNote(
        referral = referral,
        sentAt = createdAt,
        subject = "subject",
        body = "body",
        sentBy = AuthUser("sender-id", "delius", "sender.user"),
      ),
    )

    val sarsReferralData = SarsReferralData(
      referral,
      session1.appointments.toList() + session2.appointments.toList(),
      referral.intervention.dynamicFrameworkContract,
      caseNotes,
    )

    val out = json.write(SarDataDTO.from("X123456", listOf(sarsReferralData)))

    assertThat(out).isEqualToJson(
      """
          {
            "content": {
              "crn": "X123456",
              "referral": [
                {
                  "referral_number": "something",
                  "created_at": "2020-12-04T10:42:43Z",
                  "service_user_crn": "X123456",
                  "has_additional_responsibilities": true,
                  "sent_at": "2020-12-05T10:42:43Z",
                  "end_requested_at": "2020-12-06T10:42:43Z",
                  "concluded_at": "2020-12-07T10:42:43Z",
                  "draft_supplementary_risk_updated_at": "2020-12-04T10:42:43Z",
                  "withdrawal_comments": "withdrawn",
                  "status": "PRE_ICA",
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
                  "action_plans": [
                    {
                      "number_of_sessions": 3,
                      "created_at": "2020-12-04T10:42:43Z",
                      "approved_by": "bernard.beaks",
                      "action_plan_activities": [
                        {
                          "description": "action plan approved",
                          "createdAt": "2020-12-04T10:42:43Z"
                        }
                      ]
                    }
                  ],
                  "end_of_service_report": {
                    "created_at": "2020-12-04T10:42:43Z",
                    "submitted_at": "2020-12-05T10:42:43Z",
                    "further_information": "further info",
                    "end_of_service_outcomes": [
                      {
                        "progression_comments": "progressing level",
                        "additional_task_comments": "he needs interview"
                      }
                    ]
                  },
                  "case_notes": [
                    {
                      "subject": "subject",
                      "body": "body",
                      "sent_at": "2020-12-04T10:42:43Z"
                    }
                  ],
                  "probation_practitioner_details": {
                    "name": "name",
                    "pdu": "pdu",
                    "probation_office": "probation-office",
                    "role_job_title": null,
                    "establishment": null
                  },
                  "service_provider_name": "Provider"
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
    val sentAt = createdAt.plusDays(1)

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
        createdAt = createdAt,
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

    referral.hasAdditionalResponsibilities = true
    referral.sentAt = sentAt

    val actionPlan = actionPlanFactory.createApproved(
      referral = referral,
      createdAt = createdAt,
      numberOfSessions = 3,
      activities = mutableListOf(ActionPlanActivity(description = "action plan approved", createdAt = createdAt)),
    )

    referral.referenceNumber = "something"
    referral.needsInterpreter = true
    referral.interpreterLanguage = "french"
    referral.supplementaryRiskId = UUID.fromString("1c893c33-a373-435e-b13f-7efbc6206e2a")
    referral.relevantSentenceId = 123456L
    referral.actionPlans = mutableListOf(actionPlan)

    val caseNotes = listOf(
      SampleData.sampleCaseNote(
        referral = referral,
        sentAt = createdAt,
        subject = "subject",
        body = "body",
      ),
    )

    val sarsReferralData = SarsReferralData(
      referral,
      emptyList(),
      referral.intervention.dynamicFrameworkContract,
      caseNotes,
    )

    val out = json.write(SarDataDTO.from("X123456", listOf(sarsReferralData)))

    assertThat(out).isEqualToJson(
      """
        {
          "content": {
            "crn": "X123456",
            "referral": [
              {
                "referral_number": "something",
                "created_at": "2020-12-04T10:42:43Z",
                "service_user_crn": "X123456",
                "has_additional_responsibilities": true,
                "sent_at": "2020-12-05T10:42:43Z",
                "accessibility_needs": "wheelchair",
                "additional_needs_information": "english",
                "when_unavailable": "tomorrow",
                "end_requested_comments": "appointment needed",
                "appointment": [],
                "action_plans": [
                  {
                    "number_of_sessions": 3,
                    "created_at": "2020-12-04T10:42:43Z",
                    "approved_by": "bernard.beaks",
                    "action_plan_activities": [
                      {
                        "description": "action plan approved",
                        "createdAt": "2020-12-04T10:42:43Z"
                      }
                    ]
                  }
                ],
                "end_of_service_report": {
                  "created_at": "2020-12-04T10:42:43Z",
                  "end_of_service_outcomes": [
                    {
                      "progression_comments": "progressing level",
                      "additional_task_comments": "he needs interview"
                    }
                  ]
                },
                "case_notes": [
                  {
                    "subject": "subject",
                    "body": "body",
                    "sent_at": "2020-12-04T10:42:43Z"
                  }
                ],
                "probation_practitioner_details": {
                  "name": "name",
                  "pdu": "pdu",
                  "probation_office": "probation-office",
                  "role_job_title": null,
                  "establishment": null
                },
                "service_provider_name": "Provider"
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
                "referral": [],
                "draft_referral": []
              }
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `return sars data includes draft referrals when present`() {
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")

    val draftReferral = SampleData.sampleDraftReferral(
      crn = "X123456",
      serviceProviderName = "Provider",
      createdAt = createdAt,
    )

    val sarsReferralData = SarsReferralData(
      referral = null,
      appointments = emptyList(),
      dynamicFrameworkContract = null,
      caseNotes = emptyList(),
      draftReferrals = listOf(draftReferral),
    )

    val out = json.write(SarDataDTO.from("X123456", listOf(sarsReferralData)))

    assertThat(out).isEqualToJson(
      """
        {
          "content": {
            "crn": "X123456",
            "referral": [],
            "draft_referral": [
              {
                "created_at": "2020-12-04T10:42:43Z",
                "service_user_crn": "X123456",
                "accessibility_needs": "",
                "additional_needs_information": "",
                "when_unavailable": ""
              }
            ]
          }
        }
      """.trimIndent(),
    )
  }

  @Test
  fun `return sars data when some of the fields are not present`() {
    val id = UUID.randomUUID()
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val sentAt = createdAt.plusDays(1)

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
        createdAt = createdAt,
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

    val endRequestedAt = createdAt.plusDays(2)
    val concludedAt = createdAt.plusDays(3)
    referral.hasAdditionalResponsibilities = true
    referral.sentAt = sentAt
    referral.endRequestedAt = endRequestedAt
    referral.concludedAt = concludedAt
    referral.additionalRiskInformationUpdatedAt = createdAt
    referral.withdrawalComments = "withdrawn"

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
      createdAt = createdAt,
      numberOfSessions = 3,
      activities = mutableListOf(ActionPlanActivity(description = "action plan approved", createdAt = createdAt)),
    )

    referral.referenceNumber = "something"
    referral.needsInterpreter = true
    referral.interpreterLanguage = "french"
    referral.supplementaryRiskId = UUID.fromString("1c893c33-a373-435e-b13f-7efbc6206e2a")
    referral.relevantSentenceId = 123456L
    referral.actionPlans = mutableListOf(actionPlan)

    val caseNotes = listOf(
      SampleData.sampleCaseNote(
        referral = referral,
        sentAt = createdAt,
        subject = "subject",
        body = "body",
      ),
    )

    val sarsReferralData = SarsReferralData(
      referral,
      session1.appointments.toList() + session2.appointments.toList(),
      referral.intervention.dynamicFrameworkContract,
      caseNotes,
    )

    val out = json.write(SarDataDTO.from("X123456", listOf(sarsReferralData)))

    assertThat(out).isEqualToJson(
      """
            {
              "content": {
                "crn": "X123456",
                "referral": [
                  {
                    "referral_number": "something",
                    "created_at": "2020-12-04T10:42:43Z",
                    "service_user_crn": "X123456",
                    "has_additional_responsibilities": true,
                    "sent_at": "2020-12-05T10:42:43Z",
                    "end_requested_at": "2020-12-06T10:42:43Z",
                    "concluded_at": "2020-12-07T10:42:43Z",
                    "draft_supplementary_risk_updated_at": "2020-12-04T10:42:43Z",
                    "withdrawal_comments": "withdrawn",
                    "status": "PRE_ICA",
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
                    "action_plans": [
                      {
                        "number_of_sessions": 3,
                        "created_at": "2020-12-04T10:42:43Z",
                        "approved_by": "bernard.beaks",
                        "action_plan_activities": [
                          {
                            "description": "action plan approved",
                            "createdAt": "2020-12-04T10:42:43Z"
                          }
                        ]
                      }
                    ],
                    "end_of_service_report": {
                      "created_at": "2020-12-04T10:42:43Z",
                      "end_of_service_outcomes":[
                      {
                        "progression_comments": "",
                        "additional_task_comments": ""
                       }
                      ]
                    },
                    "case_notes": [
                      {
                        "subject": "subject",
                        "body": "body",
                        "sent_at": "2020-12-04T10:42:43Z"
                      }
                    ],
                    "probation_practitioner_details": {
                      "name": "name",
                      "pdu": "pdu",
                      "probation_office": "probation-office",
                      "role_job_title": null,
                      "establishment": null
                    },
                    "service_provider_name": "Provider"
                  }
                ]
              }
            }
      """.trimIndent(),
    )
  }
}
