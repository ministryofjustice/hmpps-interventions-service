package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.authorization.UserMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CaseNoteDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.CreateCaseNoteDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository.AuthUserRepository
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.CaseNoteService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralService
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AuthUserFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.CaseNoteFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.JwtTokenFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.ReferralFactory
import java.util.UUID

class CaseNoteControllerTest {
  private val caseNoteFactory = CaseNoteFactory()
  private val tokenFactory = JwtTokenFactory()
  private val authUserFactory = AuthUserFactory()
  private val referralFactory = ReferralFactory()
  private val authUserRepository = mock<AuthUserRepository>()
  private val userMapper = UserMapper(authUserRepository)
  private val caseNoteService = mock<CaseNoteService>()
  private val referralService = mock<ReferralService>()
  private val caseNoteController = CaseNoteController(userMapper, caseNoteService, referralService)

  @Nested
  inner class AddNewCaseNote {
    @Test
    fun `can add new case note to referral`() {
      val user = authUserFactory.create()
      val userToken = tokenFactory.create(user)
      val caseNote = caseNoteFactory.create(subject = "subject", body = "body")
      val referralId = caseNote.referral.id
      val createCaseNoteDTO = CreateCaseNoteDTO(referralId = referralId, subject = "subject", body = "body", sendEmail = true)

      whenever(authUserRepository.save(any())).thenReturn(user)
      whenever(referralService.getSentReferralForUser(id = referralId, user = user)).thenReturn(referralFactory.createSent(id = referralId))
      whenever(caseNoteService.createCaseNote(referralId = referralId, subject = "subject", body = "body", sendEmail = true, sentByUser = user)).thenReturn(caseNote)

      val caseNoteDTO = caseNoteController.createCaseNote(createCaseNoteDTO, userToken)
      assertThat(caseNoteDTO).isEqualTo(CaseNoteDTO.from(caseNote))
    }

    @Test
    fun `returns bad request when sent referral does not exist`() {
      val user = authUserFactory.create()
      val userToken = tokenFactory.create(user)
      val caseNote = caseNoteFactory.create(subject = "subject", body = "body")
      val referralId = caseNote.referral.id
      val createCaseNoteDTO = CreateCaseNoteDTO(referralId = referralId, subject = "subject", body = "body", sendEmail = true)
      whenever(referralService.getSentReferralForUser(id = referralId, user = user)).thenReturn(null)
      whenever(authUserRepository.save(any())).thenReturn(user)
      val e = assertThrows<ResponseStatusException> {
        caseNoteController.createCaseNote(createCaseNoteDTO, userToken)
      }
      assertThat(e.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
      assertThat(e.message).contains("sent referral not found")
    }
  }

  @Nested
  inner class GetCaseNotes {
    @Test
    fun `can get case notes`() {
      val user = authUserFactory.create()
      val userToken = tokenFactory.create(user)
      val referralId = UUID.randomUUID()
      val pageable: Pageable = Pageable.ofSize(1)
      val caseNote = caseNoteFactory.create(subject = "subject", body = "body")
      whenever(referralService.getSentReferralForUser(id = referralId, user = user)).thenReturn(referralFactory.createSent(id = referralId))
      whenever(caseNoteService.findByReferral(referralId, pageable = pageable)).thenReturn(PageImpl(listOf(caseNote)))
      whenever(authUserRepository.save(any())).thenReturn(user)

      val caseNotes = caseNoteController.getCaseNotes(pageable, referralId = referralId, authentication = userToken)
      assertThat(caseNotes.numberOfElements).isEqualTo(1)
      assertThat(caseNotes.number).isEqualTo(0)
      assertThat(caseNotes.totalPages).isEqualTo(1)
      assertThat(caseNotes.content.size).isEqualTo(1)
      assertThat(caseNotes.content[0]).isEqualTo(CaseNoteDTO.from(caseNote))
    }

    @Test
    fun `returns not found when sent referral does not exist`() {
      val user = authUserFactory.create()
      val userToken = tokenFactory.create(user)
      val referralId = UUID.randomUUID()

      whenever(referralService.getSentReferralForUser(id = referralId, user = user)).thenReturn(null)
      whenever(authUserRepository.save(any())).thenReturn(user)

      val e = assertThrows<ResponseStatusException> {
        caseNoteController.getCaseNotes(Pageable.ofSize(1), referralId = referralId, authentication = userToken)
      }
      assertThat(e.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(e.message).contains("sent referral not found")
    }
  }

  @Nested
  inner class GetCaseNote {
    @Test
    fun `can get an individual case note`() {
      val id = UUID.randomUUID()
      val caseNote = caseNoteFactory.create(id = id, subject = "subject", body = "body")
      whenever(authUserRepository.save(any())).then(AdditionalAnswers.returnsFirstArg<AuthUser>())
      whenever(caseNoteService.getCaseNoteForUser(id, caseNote.sentBy)).thenReturn(caseNote)

      val caseNoteDTO = caseNoteController.getCaseNote(id, tokenFactory.create(caseNote.sentBy))
      assertThat(caseNoteDTO.id).isEqualTo(id)
    }

    @Test
    fun `returns not found when the case note does not exist`() {
      whenever(caseNoteService.getCaseNoteForUser(any(), any())).thenReturn(null)
      whenever(authUserRepository.save(any())).thenReturn(authUserFactory.create())

      val e = assertThrows<EntityNotFoundException> {
        caseNoteController.getCaseNote(UUID.randomUUID(), tokenFactory.create(authUserFactory.createSP()))
      }
      assertThat(e.message).contains("case note not found")
    }
  }
}
