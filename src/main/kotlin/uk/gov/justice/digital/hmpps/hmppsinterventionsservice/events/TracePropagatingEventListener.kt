package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.events

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import org.springframework.context.ApplicationListener

interface TracePropagatingEventListener: ApplicationListener<TraceableEvent> {
  fun processTraceableEvent(event: TraceableEvent)

  override fun onApplicationEvent(event: TraceableEvent) {
    val context = W3CTraceContextPropagator.getInstance().extract(Context.current(), event, object: TextMapGetter<TraceableEvent> {
      override fun keys(event: TraceableEvent): MutableIterable<String> { return event.attributes.keys }
      override fun get(event: TraceableEvent?, key: String): String? { return event?.attributes?.get(key) }
    })

    context.makeCurrent().use {
      processTraceableEvent(event)
    }
  }
}