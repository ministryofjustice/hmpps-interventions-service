package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyArray
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.component.LocationMapper
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.AppointmentFactory
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.util.DeliverySessionFactory
import java.net.URI

class AppointmentEventPublisherTest {
  private val eventPublisher = mock<ApplicationEventPublisher>()
  private val locationMapper = mock<LocationMapper>()
  private val appointmentFactory = AppointmentFactory()
  private val publisher = AppointmentEventPublisher(eventPublisher, locationMapper)

  @BeforeEach
  fun setup() {
    whenever(locationMapper.getPathFromControllerMethod(any())).thenReturn("")
    whenever(locationMapper.expandPathToCurrentContextPathUrl(any(), any()))
      .thenReturn(URI.create("http://localhost/sent-referral/123/supplier-assessment"))
  }

  @Test
  fun `builds a supplier assessment appointment attendance recorded event and publishes it`() {
    val appointment = appointmentFactory.create()

    publisher.attendanceRecordedEvent(appointment, false, AppointmentType.SUPPLIER_ASSESSMENT)

    val eventCaptor = argumentCaptor<AppointmentEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(AppointmentEventType.ATTENDANCE_RECORDED)
    Assertions.assertThat(event.appointmentType).isSameAs(AppointmentType.SUPPLIER_ASSESSMENT)
    Assertions.assertThat(event.appointment).isSameAs(appointment)
    Assertions.assertThat(event.detailUrl).isEqualTo("http://localhost/sent-referral/123/supplier-assessment")
    Assertions.assertThat(event.notifyPP).isFalse
  }

  @Test
  fun `builds a delivery session appointment attendance recorded event and publishes it`() {
    whenever(locationMapper.expandPathToCurrentContextPathUrl(any(), anyArray<Object>()))
      .thenReturn(URI.create("http://localhost/referral/123/sessions/1"))

    val appointment = appointmentFactory.create()
    val deliverySession = DeliverySessionFactory().createScheduled()

    publisher.attendanceRecordedEvent(appointment, false, AppointmentType.SERVICE_DELIVERY, deliverySession)

    val eventCaptor = argumentCaptor<AppointmentEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(AppointmentEventType.ATTENDANCE_RECORDED)
    Assertions.assertThat(event.appointmentType).isSameAs(AppointmentType.SERVICE_DELIVERY)
    Assertions.assertThat(event.appointment).isSameAs(appointment)
    Assertions.assertThat(event.detailUrl).isEqualTo("http://localhost/referral/123/sessions/1")
    Assertions.assertThat(event.deliverySession).isEqualTo(deliverySession)
    Assertions.assertThat(event.notifyPP).isFalse
  }

  @Test
  fun `builds a supplier assessment appointment session feedback recorded event and publishes it`() {
    val appointment = appointmentFactory.create()

    publisher.sessionFeedbackRecordedEvent(appointment, notifyProbationPractitionerOfBehaviour = true, notifyProbationPractitionerOfConcerns = true, AppointmentType.SUPPLIER_ASSESSMENT)

    val eventCaptor = argumentCaptor<AppointmentEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(AppointmentEventType.SESSION_FEEDBACK_RECORDED)
    Assertions.assertThat(event.appointment).isSameAs(appointment)
    Assertions.assertThat(event.detailUrl).isEqualTo("http://localhost/sent-referral/123/supplier-assessment")
    Assertions.assertThat(event.notifyPP).isTrue
  }

  @Test
  fun `builds a delivery session appointment session feedback recorded event and publishes it`() {
    whenever(locationMapper.expandPathToCurrentContextPathUrl(any(), anyArray<Object>()))
      .thenReturn(URI.create("http://localhost/referral/123/sessions/1"))

    val appointment = appointmentFactory.create()
    val deliverySession = DeliverySessionFactory().createScheduled()

    publisher.sessionFeedbackRecordedEvent(appointment, notifyProbationPractitionerOfBehaviour = true, notifyProbationPractitionerOfConcerns = true, AppointmentType.SERVICE_DELIVERY, deliverySession)

    val eventCaptor = argumentCaptor<AppointmentEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(AppointmentEventType.SESSION_FEEDBACK_RECORDED)
    Assertions.assertThat(event.appointmentType).isSameAs(AppointmentType.SERVICE_DELIVERY)
    Assertions.assertThat(event.appointment).isSameAs(appointment)
    Assertions.assertThat(event.detailUrl).isEqualTo("http://localhost/referral/123/sessions/1")
    Assertions.assertThat(event.deliverySession).isEqualTo(deliverySession)
    Assertions.assertThat(event.notifyPP).isTrue
  }

  @Test
  fun `builds a supplier assessment appointment feedback event and publishes it`() {
    val appointment = appointmentFactory.create()

    publisher.appointmentFeedbackRecordedEvent(appointment, notifyProbationPractitionerOfBehaviour = true, notifyProbationPractitionerOfConcerns = true, AppointmentType.SUPPLIER_ASSESSMENT)

    val eventCaptor = argumentCaptor<AppointmentEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(AppointmentEventType.APPOINTMENT_FEEDBACK_RECORDED)
    Assertions.assertThat(event.appointmentType).isSameAs(AppointmentType.SUPPLIER_ASSESSMENT)
    Assertions.assertThat(event.appointment).isSameAs(appointment)
    Assertions.assertThat(event.detailUrl).isEqualTo("http://localhost/sent-referral/123/supplier-assessment")
    Assertions.assertThat(event.notifyPP).isTrue
  }

  @Test
  fun `builds a delivery session appointment feedback event and publishes it`() {
    whenever(locationMapper.expandPathToCurrentContextPathUrl(any(), anyArray<Object>()))
      .thenReturn(URI.create("http://localhost/referral/123/sessions/1"))

    val appointment = appointmentFactory.create()
    val deliverySession = DeliverySessionFactory().createScheduled()

    publisher.appointmentFeedbackRecordedEvent(appointment, notifyProbationPractitionerOfBehaviour = true, notifyProbationPractitionerOfConcerns = true,   AppointmentType.SERVICE_DELIVERY, deliverySession)

    val eventCaptor = argumentCaptor<AppointmentEvent>()
    verify(eventPublisher).publishEvent(eventCaptor.capture())
    val event = eventCaptor.firstValue

    Assertions.assertThat(event.source).isSameAs(publisher)
    Assertions.assertThat(event.type).isSameAs(AppointmentEventType.APPOINTMENT_FEEDBACK_RECORDED)
    Assertions.assertThat(event.appointmentType).isSameAs(AppointmentType.SERVICE_DELIVERY)
    Assertions.assertThat(event.appointment).isSameAs(appointment)
    Assertions.assertThat(event.detailUrl).isEqualTo("http://localhost/referral/123/sessions/1")
    Assertions.assertThat(event.deliverySession).isEqualTo(deliverySession)
    Assertions.assertThat(event.notifyPP).isTrue
  }
}
