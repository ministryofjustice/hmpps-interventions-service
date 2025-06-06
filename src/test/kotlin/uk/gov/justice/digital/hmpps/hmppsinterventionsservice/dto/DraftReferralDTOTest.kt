package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DesiredOutcome
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralDetailsFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ServiceCategoryFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@JsonTest
class DraftReferralDTOTest(@Autowired private val json: JacksonTester<DraftReferralDTO>) {
  private val referralFactory = ReferralFactory()
  private val referralDetailsFactory = ReferralDetailsFactory()
  private val authUserFactory = AuthUserFactory()
  private val serviceCategoryFactory = ServiceCategoryFactory()

  @Test
  fun `test serialization of newly created referral`() {
    val referral = referralFactory.createDraft(
      id = UUID.fromString("3B9ED289-8412-41A9-8291-45E33E60276C"),
      createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00"),
    )

    val out = json.write(DraftReferralDTO.from(referral))
    assertThat(out).isEqualToJson(
      """
      {
        "id": "3b9ed289-8412-41a9-8291-45e33e60276c",
        "createdAt": "2020-12-04T10:42:43Z",
        "serviceUser": {
          "crn": "X123456"
        },
        "serviceProvider": {
          "name": "Harmony Living"
        },
        "serviceCategoryIds": null,
        "desiredOutcomes": null,
        "complexityLevels": null,
        "contractTypeName": "Accommodation"
      }
    """,
    )
  }

  @Test
  fun `test serialization of referral with empty collections`() {
    val referral = referralFactory.createDraft(
      complexityLevelIds = mutableMapOf(),
      desiredOutcomes = emptyList(),
      selectedServiceCategories = mutableSetOf(),
    )

    val out = json.write(DraftReferralDTO.from(referral))
    assertThat(out).isEqualToJson(
      """
      {
        "serviceCategoryIds": null,
        "desiredOutcomes": null,
        "complexityLevels": null
      }
    """,
    )
  }

  @Test
  fun `test serialization of referral with populated collections`() {
    val serviceCategory = serviceCategoryFactory.create(id = UUID.fromString("e30c24e5-3aa7-4820-82ee-b028909876e6"))
    val complexityLevelUUID = UUID.fromString("11c6d1f1-a22e-402a-b343-e57595876857")
    val desiredOutcomeUUID = UUID.fromString("5a9d6e60-c314-4bf9-bf1e-b42b366b9398")
    val referral = referralFactory.createDraft(
      complexityLevelIds = mutableMapOf(serviceCategory.id to complexityLevelUUID),
      desiredOutcomes = listOf(DesiredOutcome(desiredOutcomeUUID, "", serviceCategory.id, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf())),
      selectedServiceCategories = mutableSetOf(serviceCategory),
    )

    val out = json.write(DraftReferralDTO.from(referral))
    assertThat(out).isEqualToJson(
      """
      {
        "serviceCategoryIds": ["e30c24e5-3aa7-4820-82ee-b028909876e6"],
        "desiredOutcomes": [{
           "serviceCategoryId": "e30c24e5-3aa7-4820-82ee-b028909876e6",
           "desiredOutcomesIds": ["5a9d6e60-c314-4bf9-bf1e-b42b366b9398"]
        }],
        "complexityLevels": [{
           "serviceCategoryId": "e30c24e5-3aa7-4820-82ee-b028909876e6",
           "complexityLevelId": "11c6d1f1-a22e-402a-b343-e57595876857"
        }]
      }
    """,
    )
  }

  @Test
  fun `test serialization of referral with completionDeadline`() {
    val referralID = UUID.fromString("3B9ED289-8412-41A9-8291-45E33E60276C")
    val createdAt = OffsetDateTime.parse("2020-12-04T10:42:43+00:00")
    val createdBy = authUserFactory.createPP()
    val referral = referralFactory.createDraft(
      serviceUserCRN = "X123456",
      id = referralID,
      createdAt = createdAt,
      createdBy = createdBy,
      referralDetail = referralDetailsFactory.create(completionDeadline = LocalDate.of(2021, 2, 12), createdBy = createdBy, createdAt = createdAt, referralId = referralID),
    )

    val out = json.write(DraftReferralDTO.from(referral))
    assertThat(out).isEqualToJson(
      """
      {
        "id": "3b9ed289-8412-41a9-8291-45e33e60276c",
        "createdAt": "2020-12-04T10:42:43Z",
        "completionDeadline": "2021-02-12",
        "serviceUser": {
          "crn": "X123456"
        },
      }
    """,
    )
  }

  @Test
  fun `test deserialization of partial referral`() {
    val draftReferral = json.parseObject(
      """
      {"completionDeadline": "2021-02-10"}
    """,
    )
    assertThat(draftReferral.completionDeadline).isEqualTo(LocalDate.of(2021, 2, 10))
  }

  @Test
  fun `test deserialization of relevant sentence id`() {
    val draftReferral = json.parseObject(
      """
      {"relevantSentenceId": 123456789}
    """,
    )
    assertThat(draftReferral.relevantSentenceId).isEqualTo(123456789)
  }

  @Nested inner class RiskInformation {
    // this field has some funky JsonView annotations going on,
    // so double check it's working as expected

    @Test
    fun `test serialization of additionalRiskInformation in draft referrals`() {
      val referral = referralFactory.createDraft(id = UUID.fromString("3b9ed289-8412-41a9-8291-45e33e60276c"))

      val out = json.write(DraftReferralDTO.from(referral))
      assertThat(out).isEqualToJson(
        """
        {
          "id": "3b9ed289-8412-41a9-8291-45e33e60276c",
          "completionDeadline": null,
          "additionalRiskInformation": null
        }
        """,
      )
    }

    @Test
    fun `test serialization of additionalRiskInformation in sent referrals`() {
      val id = UUID.fromString("3b9ed289-8412-41a9-8291-45e33e60276c")
      val referral = referralFactory.createSent(id = id)

      val out = json.forView(Views.SentReferral::class.java)
        .write(DraftReferralDTO.from(referral))
      assertThat(out).isEqualToJson(
        """
        {
          "id": "3b9ed289-8412-41a9-8291-45e33e60276c",
          "completionDeadline": null
        }
        """,
      )
      assertThat(out).doesNotHaveJsonPath("$.additionalRiskInformation")
    }
  }

  @Nested
  inner class Ordering {
    val uuid1 = UUID.fromString("10000000-0106-4bcf-9da4-2a3c8c062114")
    val uuid2 = UUID.fromString("20000000-0106-4bcf-9da4-2a3c8c062114")
    val uuid3 = UUID.fromString("30000000-0106-4bcf-9da4-2a3c8c062114")
    val uuid4 = UUID.fromString("40000000-0106-4bcf-9da4-2a3c8c062114")
    val uuid5 = UUID.fromString("50000000-0106-4bcf-9da4-2a3c8c062114")
    val uuid6 = UUID.fromString("60000000-0106-4bcf-9da4-2a3c8c062114")

    @Test
    fun `service category ids are always the same order`() {
      val serviceCat1 = serviceCategoryFactory.create(id = uuid1)
      val serviceCat2 = serviceCategoryFactory.create(id = uuid2)
      val serviceCat3 = serviceCategoryFactory.create(id = uuid3)
      val serviceCat4 = serviceCategoryFactory.create(id = uuid4)
      val serviceCat5 = serviceCategoryFactory.create(id = uuid5)
      val referral = referralFactory.createDraft(selectedServiceCategories = mutableSetOf(serviceCat5, serviceCat3, serviceCat4, serviceCat2, serviceCat1))
      val referralDTO = DraftReferralDTO.from(referral)

      assertThat(referralDTO.serviceCategoryIds).hasSize(5)
      assertThat(referralDTO.serviceCategoryIds!!.elementAt(0)).isEqualTo(serviceCat1.id)
      assertThat(referralDTO.serviceCategoryIds!!.elementAt(1)).isEqualTo(serviceCat2.id)
      assertThat(referralDTO.serviceCategoryIds!!.elementAt(2)).isEqualTo(serviceCat3.id)
      assertThat(referralDTO.serviceCategoryIds!!.elementAt(3)).isEqualTo(serviceCat4.id)
      assertThat(referralDTO.serviceCategoryIds!!.elementAt(4)).isEqualTo(serviceCat5.id)
    }

    @Test
    fun `complexity levels are always in the same order`() {
      val map: MutableMap<UUID, UUID> = mutableMapOf(uuid5 to uuid5, uuid3 to uuid3, uuid4 to uuid4, uuid2 to uuid2, uuid1 to uuid1)
      val referral = referralFactory.createDraft(complexityLevelIds = map)
      val referralDTO = DraftReferralDTO.from(referral)

      assertThat(referralDTO.complexityLevels).hasSize(5)
      assertThat(referralDTO.complexityLevels!!.elementAt(0).serviceCategoryId).isEqualTo(uuid1)
      assertThat(referralDTO.complexityLevels!!.elementAt(1).serviceCategoryId).isEqualTo(uuid2)
      assertThat(referralDTO.complexityLevels!!.elementAt(2).serviceCategoryId).isEqualTo(uuid3)
      assertThat(referralDTO.complexityLevels!!.elementAt(3).serviceCategoryId).isEqualTo(uuid4)
      assertThat(referralDTO.complexityLevels!!.elementAt(4).serviceCategoryId).isEqualTo(uuid5)
    }

    @Test
    fun `desired outcomes are always in the same order`() {
      val desiredOutcome1 = DesiredOutcome(id = uuid1, "", serviceCategoryId = uuid1, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf())
      val desiredOutcome2 = DesiredOutcome(id = uuid2, "", serviceCategoryId = uuid1, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf())
      val desiredOutcome3 = DesiredOutcome(id = uuid3, "", serviceCategoryId = uuid1, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf())

      val desiredOutcome4 = DesiredOutcome(id = uuid4, "", serviceCategoryId = uuid2, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf())
      val desiredOutcome5 = DesiredOutcome(id = uuid5, "", serviceCategoryId = uuid2, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf())
      val desiredOutcome6 = DesiredOutcome(id = uuid6, "", serviceCategoryId = uuid2, deprecatedAt = null, desiredOutcomeFilterRules = mutableSetOf())
      val referral = referralFactory.createDraft(desiredOutcomes = listOf(desiredOutcome5, desiredOutcome3, desiredOutcome4, desiredOutcome2, desiredOutcome1, desiredOutcome6))
      val referralDTO = DraftReferralDTO.from(referral)

      assertThat(referralDTO.desiredOutcomes).hasSize(2)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(0).serviceCategoryId).isEqualTo(uuid1)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(0).desiredOutcomesIds).hasSize(3)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(0).desiredOutcomesIds.elementAt(0)).isEqualTo(uuid1)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(0).desiredOutcomesIds.elementAt(1)).isEqualTo(uuid2)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(0).desiredOutcomesIds.elementAt(2)).isEqualTo(uuid3)

      assertThat(referralDTO.desiredOutcomes!!.elementAt(1).serviceCategoryId).isEqualTo(uuid2)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(1).desiredOutcomesIds).hasSize(3)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(1).desiredOutcomesIds.elementAt(0)).isEqualTo(uuid4)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(1).desiredOutcomesIds.elementAt(1)).isEqualTo(uuid5)
      assertThat(referralDTO.desiredOutcomes!!.elementAt(1).desiredOutcomesIds.elementAt(2)).isEqualTo(uuid6)
    }
  }
}
