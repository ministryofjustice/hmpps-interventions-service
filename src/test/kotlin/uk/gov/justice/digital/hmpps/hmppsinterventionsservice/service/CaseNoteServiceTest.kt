package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.config.AccessError
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.CaseNoteEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.CaseNoteRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.ReferralRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CaseNoteFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.RepositoryTest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RepositoryTest
class CaseNoteServiceTest @Autowired constructor(
  val entityManager: TestEntityManager,
  val caseNoteRepository: CaseNoteRepository,
  val referralRepository: ReferralRepository,
  val authUserRepository: AuthUserRepository,
) {
  private val referralService = mock<ReferralService>()
  private val caseNoteEventPublisher = mock<CaseNoteEventPublisher>()
  private val userFactory = AuthUserFactory(entityManager)
  private val caseNoteService = CaseNoteService(
    caseNoteRepository,
    referralRepository,
    referralService,
    authUserRepository,
    caseNoteEventPublisher,
  )
  private val caseNoteFactory = CaseNoteFactory(entityManager)
  private val referralFactory = ReferralFactory(entityManager)

  @Nested
  inner class CreateCaseNote {
    @Test
    fun `can create case note`() {
      val referral = referralFactory.createSent()
      val user = userFactory.create()
      val sendEmail = false
      val caseNote = caseNoteService.createCaseNote(referral.id, subject = "subject", body = "body", sendEmail = sendEmail, user)
      val savedCaseNote = caseNoteRepository.findById(caseNote.id).get()
      assertThat(caseNote).isEqualTo(savedCaseNote)
      assertThat(caseNote.referral.id).isEqualTo(referral.id)
      assertThat(caseNote.subject).isEqualTo("subject")
      assertThat(caseNote.body).isEqualTo("body")
      assertThat(caseNote.sentAt).isNotNull()
      assertThat(caseNote.sentBy).isEqualTo(user)

      verify(caseNoteEventPublisher).caseNoteSentEvent(caseNote, sendEmail)
    }
  }

  @Nested
  inner class FindByReferral {
    @Test
    fun `can find case note by referral id with no pageable parameter`() {
      val referral = referralFactory.createSent()
      val caseNote = caseNoteFactory.create(referral = referral, subject = "subject", body = "body")

      val caseNotes = caseNoteService.findByReferral(referralId = referral.id, null)

      assertThat(caseNotes.totalPages).isEqualTo(1)
      assertThat(caseNotes.numberOfElements).isEqualTo(1)
      assertThat(caseNotes.number).isEqualTo(0)
      assertThat(caseNotes.content).isEqualTo(listOf(caseNote))
    }

    @Test
    fun `can return the correct page of case notes with provided pageable`() {
      val referral = referralFactory.createSent()
      val caseNote5 = caseNoteFactory.create(
        referral = referral,
        subject = "subject",
        body = "body",
        sentAt = OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 5, 0), ZoneOffset.MAX),
      )
      val caseNote1 = caseNoteFactory.create(
        referral = referral,
        subject = "subject",
        body = "body",
        sentAt = OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 1, 0), ZoneOffset.MAX),
      )
      val caseNote2 = caseNoteFactory.create(
        referral = referral,
        subject = "subject",
        body = "body",
        sentAt = OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 2, 0), ZoneOffset.MAX),
      )
      val caseNote4 = caseNoteFactory.create(
        referral = referral,
        subject = "subject",
        body = "body",
        sentAt = OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 4, 0), ZoneOffset.MAX),
      )
      val caseNote7 = caseNoteFactory.create(
        referral = referral,
        subject = "subject",
        body = "body",
        sentAt = OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 7, 0), ZoneOffset.MAX),
      )
      val caseNote6 = caseNoteFactory.create(
        referral = referral,
        subject = "subject",
        body = "body",
        sentAt = OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 6, 0), ZoneOffset.MAX),
      )
      val caseNote3 = caseNoteFactory.create(
        referral = referral,
        subject = "subject",
        body = "body",
        sentAt = OffsetDateTime.of(LocalDateTime.of(2021, 1, 1, 3, 0), ZoneOffset.MAX),
      )

      val caseNotes = caseNoteService.findByReferral(
        referralId = referral.id,
        PageRequest.of(
          1,
          3,
          Sort.by("sentAt"),
        ),
      )

      assertThat(caseNotes.totalPages).isEqualTo(3)
      assertThat(caseNotes.numberOfElements).isEqualTo(3)
      assertThat(caseNotes.number).isEqualTo(1)
      assertThat(caseNotes.content).isEqualTo(listOf(caseNote4, caseNote5, caseNote6))
    }

    @Test
    fun `returns empty when page is out of bounds`() {
      val referral = referralFactory.createSent()
      caseNoteFactory.create(referral = referral, subject = "subject", body = "body")

      val caseNotes = caseNoteService.findByReferral(referralId = referral.id, PageRequest.of(2, 1))

      assertThat(caseNotes.totalPages).isEqualTo(1)
      assertThat(caseNotes.numberOfElements).isEqualTo(0)
      assertThat(caseNotes.number).isEqualTo(2)
      assertThat(caseNotes.content).isEmpty()
    }
  }

  @Nested
  inner class GetCaseNoteForUser {
    @Test
    fun `can get case note by id`() {
      val referral = referralFactory.createSent()
      val caseNote =
        caseNoteService.createCaseNote(referral.id, subject = "subject", body = "body", sendEmail = false, sentByUser = referral.sentBy!!)

      val retrievedCaseNote = caseNoteService.getCaseNoteForUser(caseNote.id, referral.sentBy!!)
      assertThat(retrievedCaseNote).isEqualTo(caseNote)
    }

    @Test
    fun `user can't get case note without access to referral`() {
      val referral = referralFactory.createSent()
      val caseNote =
        caseNoteService.createCaseNote(referral.id, subject = "subject", body = "body", sendEmail = false, sentByUser = referral.sentBy!!)
      whenever(
        referralService.getSentReferralForUser(
          caseNote.id,
          caseNote.sentBy,
        ),
      ).thenThrow(AccessError(caseNote.sentBy, "denied", emptyList()))

      assertThrows<AccessError> { caseNoteService.getCaseNoteForUser(caseNote.id, referral.sentBy!!) }
    }
  }
}
