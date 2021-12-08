package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

open class TraceableEvent(source: Any): ApplicationEvent(source) {
  val attributes: MutableMap<String, String> = mutableMapOf()
}

@Component
class TracePropagatingEventPublisher(
  private val context: ApplicationContext,
): ApplicationEventPublisher {
  fun publishEvent(traceableEvent: TraceableEvent) {
    W3CTraceContextPropagator.getInstance().inject(Context.current(), traceableEvent) { event, key, value ->
      event?.attributes?.put(key, value)
    }
    publishEvent(traceableEvent as Any)
  }

  override fun publishEvent(event: Any) {
    context.publishEvent(event)
  }
}