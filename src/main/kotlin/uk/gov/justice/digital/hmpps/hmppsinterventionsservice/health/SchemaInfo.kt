package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.health

import org.springframework.stereotype.Component
import schemacrawler.tools.command.text.diagram.options.DiagramOutputFormat
import schemacrawler.tools.executable.SchemaCrawlerExecutable
import schemacrawler.tools.options.OutputOptionsBuilder
import java.io.File
import javax.sql.DataSource

@Component
class SchemaInfo(private val dataSource: DataSource) {
  private val schemaFile = File.createTempFile("schema", "html")

  fun schemaDocument(): String {
    if (schemaFile.exists() && schemaFile.length() > 0)
      return schemaFile.readText()

    val output = OutputOptionsBuilder.newOutputOptions(DiagramOutputFormat.htmlx, schemaFile.toPath())
    val executable = SchemaCrawlerExecutable("schema")
    executable.outputOptions = output
    executable.setConnection(dataSource.connection)
    executable.execute()

    return schemaFile.readText()
  }
}
