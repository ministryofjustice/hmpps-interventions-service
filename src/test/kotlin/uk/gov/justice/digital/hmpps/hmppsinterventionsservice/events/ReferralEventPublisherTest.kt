package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller.ReferralController
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.ASSIGNED
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events.ReferralEventType.SENT
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.SampleData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralConcludedState
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralWithdrawalState
import java.net.URI

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
    assertThat(event.referral).isSameAs(referral)
    assertThat(event.detailUrl).isEqualTo(uri.toString())
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
