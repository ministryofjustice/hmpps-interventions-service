package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.PersistenceUnitUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.ReferralController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.AuthUserDTO
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.ASSIGNED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.SENT
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralAssignment
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralWithdrawalState
import java.net.URI
import java.time.OffsetDateTime

class ReferralEventPublisherTest {
  private val eventPublisher = mock<ApplicationEventPublisher>()
  private val locationMapper = mock<LocationMapper>()
  private val targetUser = mock<AuthUser>()

  @Test
  fun `builds an referral sent event and publishes it`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val uri = URI.create("http://localhost/sent-referral/" + referral.id)
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/sent-referral/{id}", referral.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)).thenReturn("/sent-referral/{id}")
    val publisher = ReferralEventPublisher(eventPublisher, locationMapper)

    publisher.referralSentEvent(referral)

    val eventCaptor = argumentCaptor<ReferralEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isSameAs(SENT)
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
  }

  @Test
  fun `builds an referral assign event and publishes it`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val assignment = ReferralAssignment(OffsetDateTime.now(), AuthUser("actor", "auth", "actor"), AuthUser("assignee", "auth", "assignee"))
    referral.assignments.add(assignment)
    val uri = URI.create("http://localhost/sent-referral/" + referral.id)
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/sent-referral/{id}", referral.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)).thenReturn("/sent-referral/{id}")
    val publisher = ReferralEventPublisher(eventPublisher, locationMapper)

    publisher.referralAssignedEvent(referral)

    val eventCaptor = argumentCaptor<ReferralEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isSameAs(ASSIGNED)
    assertThat(event.assignment).isEqualTo(
      ReferralAssignmentSnapshot(assignment.assignedAt, AuthUserDTO.from(assignment.assignedBy), AuthUserDTO.from(assignment.assignedTo)),
    )
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
  }

  @Test
  fun `listener reloads preserve the original assignment snapshot and load local associations`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val original = ReferralAssignment(OffsetDateTime.now(), AuthUser("actor-a", "auth", "actor-a"), AuthUser("assignee-a", "auth", "assignee-a"))
    referral.assignments.add(original)
    val event = ReferralEvent("source", ASSIGNED, referral, "http://localhost/sent-referral/${referral.id}")
    original.superseded = true
    val latest = ReferralAssignment(original.assignedAt.plusSeconds(1), AuthUser("actor-b", "auth", "actor-b"), AuthUser("assignee-b", "auth", "assignee-b"))
    referral.assignments.add(latest)

    repeat(2) { listener ->
      val entityManager = mock<EntityManager>()
      val entityManagerFactory = mock<EntityManagerFactory>()
      val persistenceUnitUtil = mock<PersistenceUnitUtil>()
      val localReferral = SampleData.sampleReferral("CRN1234", "Listener $listener provider", id = referral.id)
      localReferral.assignments.add(latest)
      whenever(entityManager.entityManagerFactory).thenReturn(entityManagerFactory)
      whenever(entityManagerFactory.persistenceUnitUtil).thenReturn(persistenceUnitUtil)
      whenever(persistenceUnitUtil.getIdentifier(referral)).thenReturn(referral.id)
      whenever(entityManager.find(Referral::class.java, referral.id)).thenReturn(localReferral)

      val reloaded = event.reloadIn(entityManager)

      assertThat(reloaded.referral).isSameAs(localReferral)
      assertThat(reloaded.referral.intervention).isSameAs(localReferral.intervention)
      assertThat(reloaded.referral.currentAssignment).isSameAs(latest)
      assertThat(reloaded.assignment).isSameAs(event.assignment)
      assertThat(reloaded.assignment).isEqualTo(
        ReferralAssignmentSnapshot(original.assignedAt, AuthUserDTO.from(original.assignedBy), AuthUserDTO.from(original.assignedTo)),
      )
      assertThat(reloaded.source).isEqualTo(event.source)
      assertThat(reloaded.type).isEqualTo(ASSIGNED)
      assertThat(reloaded.detailUrl).isEqualTo(event.detailUrl)
    }
  }

  @Test
  fun `assigned event requires an assignment`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    assertThrows<IllegalArgumentException> { ReferralEvent("source", ASSIGNED, referral, "http://localhost") }
  }

  @Test
  fun `builds an referral concluded event and publishes it`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val uri = URI.create("http://localhost/sent-referral/" + referral.id)
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/sent-referral/{id}", referral.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)).thenReturn("/sent-referral/{id}")
    val publisher = ReferralEventPublisher(eventPublisher, locationMapper)

    publisher.referralConcludedEvent(referral, ReferralConcludedState.COMPLETED, ReferralWithdrawalState.PRE_ICA_WITHDRAWAL)

    val eventCaptor = argumentCaptor<ReferralConcludedEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isEqualTo(ReferralConcludedState.COMPLETED)
    assertThat(event.referralWithdrawalState).isEqualTo(ReferralWithdrawalState.PRE_ICA_WITHDRAWAL)
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
  }

  @Test
  fun `builds an referral pp name updated event and publishes it`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val uri = URI.create("http://localhost/sent-referral/" + referral.id)
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/sent-referral/{id}", referral.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)).thenReturn("/sent-referral/{id}")
    val publisher = ReferralEventPublisher(eventPublisher, locationMapper)

    publisher.referralProbationPractitionerNameChangedEvent(referral, "new", "original", targetUser)

    val eventCaptor = argumentCaptor<ReferralEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isEqualTo(ReferralEventType.PROBATION_PRACTITIONER_NAME_AMENDED)
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
    assertThat(event.data["newProbationPractitionerName"]).isEqualTo("new")
    assertThat(event.data["oldProbationPractitionerName"]).isEqualTo("original")
  }

  @Test
  fun `builds an referral pp email updated event and publishes it`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val uri = URI.create("http://localhost/sent-referral/" + referral.id)
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/sent-referral/{id}", referral.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)).thenReturn("/sent-referral/{id}")
    val publisher = ReferralEventPublisher(eventPublisher, locationMapper)

    publisher.referralProbationPractitionerEmailChangedEvent(referral, "new@somewhere.com", "original@somewhere.com", targetUser)

    val eventCaptor = argumentCaptor<ReferralEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isEqualTo(ReferralEventType.PROBATION_PRACTITIONER_EMAIL_AMENDED)
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
    assertThat(event.data["newProbationPractitionerEmail"]).isEqualTo("new@somewhere.com")
    assertThat(event.data["oldProbationPractitionerEmail"]).isEqualTo("original@somewhere.com")
  }

  @Test
  fun `builds an referral pp phone number updated event and publishes it`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val uri = URI.create("http://localhost/sent-referral/" + referral.id)
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/sent-referral/{id}", referral.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)).thenReturn("/sent-referral/{id}")
    val publisher = ReferralEventPublisher(eventPublisher, locationMapper)

    publisher.referralProbationPractitionerPhoneNumberChangedEvent(referral, "11111111111", "00000000000", targetUser)

    val eventCaptor = argumentCaptor<ReferralEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isEqualTo(ReferralEventType.PROBATION_PRACTITIONER_PHONE_NUMBER_AMENDED)
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
    assertThat(event.data["newProbationPractitionerPhoneNumber"]).isEqualTo("11111111111")
    assertThat(event.data["oldProbationPractitionerPhoneNumber"]).isEqualTo("00000000000")
  }

  @Test
  fun `builds an referral pp team phone number updated event and publishes it`() {
    val referral = SampleData.sampleReferral("CRN1234", "Service Provider Name")
    val uri = URI.create("http://localhost/sent-referral/" + referral.id)
    whenever(locationMapper.expandPathToCurrentContextPathUrl("/sent-referral/{id}", referral.id)).thenReturn(uri)
    whenever(locationMapper.getPathFromControllerMethod(ReferralController::getSentReferral)).thenReturn("/sent-referral/{id}")
    val publisher = ReferralEventPublisher(eventPublisher, locationMapper)

    publisher.referralProbationPractitionerTeamPhoneNumberChangedEvent(referral, "11111111111", "00000000000", targetUser)

    val eventCaptor = argumentCaptor<ReferralEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    assertThat(event.source).isSameAs(publisher)
    assertThat(event.type).isEqualTo(ReferralEventType.PROBATION_PRACTITIONER_TEAM_PHONE_NUMBER_AMENDED)
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
    assertThat(event.data["newProbationPractitionerTeamPhoneNumber"]).isEqualTo("11111111111")
    assertThat(event.data["oldProbationPractitionerTeamPhoneNumber"]).isEqualTo("00000000000")
  }
}
