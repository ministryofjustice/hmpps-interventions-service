package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.controller

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.health.SchemaInfo

@Controller
@ConditionalOnProperty("feature.schemainfo")
class SchemaInfoController(
  private val schemaInfo: SchemaInfo,
) {
  @RequestMapping("/meta/schema", produces = [MediaType.TEXT_HTML_VALUE])
  @ResponseBody
  fun schema(): String {
    return schemaInfo.schemaDocument()
  }
}
