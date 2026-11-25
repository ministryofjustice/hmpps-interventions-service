package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting

import jakarta.persistence.EntityManagerFactory
import mu.KLogging
import net.logstash.logback.argument.StructuredArguments
import org.apache.commons.csv.CSVFormat
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.job.parameters.JobParametersIncrementer
import org.springframework.batch.core.listener.ChunkListener
import org.springframework.batch.core.listener.StepExecutionListener
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.core.step.skip.SkipPolicy
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.file.FlatFileHeaderCallback
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder
import org.springframework.batch.infrastructure.item.file.transform.BeanWrapperFieldExtractor
import org.springframework.batch.infrastructure.item.file.transform.ExtractorLineAggregator
import org.springframework.batch.infrastructure.item.file.transform.RecursiveCollectionLineAggregator
import org.springframework.core.io.WritableResource
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.ndmis.performance.NdmisReferralPerformanceReportJobConfiguration
import java.io.Writer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@Component
class BatchUtils {
  private val zoneOffset = ZoneOffset.UTC

  fun parseLocalDateToDate(date: LocalDate): Date {
    // convert the input date into a timestamp zoned in UTC
    // (for converting LocalDates in request params to batch job params
    return Date.from(date.atStartOfDay().atOffset(zoneOffset).toInstant())
  }

  fun parseDateToOffsetDateTime(date: Date): OffsetDateTime {
    // (for converting Dates in batch job params to sql query params)
    return date.toInstant().atOffset(zoneOffset)
  }

  private fun <T : Any> csvFileWriterBase(
    name: String,
    resource: WritableResource,
    headers: List<String>,
  ): FlatFileItemWriterBuilder<T> = FlatFileItemWriterBuilder<T>()
    .name(name)
    .resource(resource)
    .headerCallback(HeaderWriter(headers.joinToString(",")))

  fun <T : Any> csvFileWriter(
    name: String,
    resource: WritableResource,
    headers: List<String>,
    fields: List<String>,
  ): FlatFileItemWriter<T> = csvFileWriterBase<T>(name, resource, headers)
    .lineAggregator(CsvLineAggregator(fields))
    .build()

  fun <T : Any> recursiveCollectionCsvFileWriter(
    name: String,
    resource: WritableResource,
    headers: List<String>,
    fields: List<String>,
  ): FlatFileItemWriter<Collection<T>> = csvFileWriterBase<Collection<T>>(name, resource, headers)
    .lineAggregator(
      RecursiveCollectionLineAggregator<T>().apply {
        setDelegate(CsvLineAggregator<T>(fields))
      },
    ).build()
}

class HeaderWriter(private val header: String) : FlatFileHeaderCallback {
  override fun writeHeader(writer: Writer) {
    writer.write(header)
  }
}

interface SentReferralProcessor<T : Any> : ItemProcessor<Referral, T> {
  companion object : KLogging()

  fun processSentReferral(referral: Referral): T?

  override fun process(referral: Referral): T? {
    logger.debug("processing referral {}", StructuredArguments.kv("referralId", referral.id))
    return processSentReferral(referral)
  }
}

class TimestampIncrementer : JobParametersIncrementer {
  override fun getNext(inputParams: JobParameters?): JobParameters {
    val params = inputParams ?: JobParameters()

    if (params.getParameter("timestamp") != null) {
      return params
    }

    return JobParametersBuilder(params)
      .addLong("timestamp", Instant.now().epochSecond)
      .toJobParameters()
  }
}

class OutputPathIncrementer : JobParametersIncrementer {
  override fun getNext(inputParams: JobParameters?): JobParameters {
    val params = inputParams ?: JobParameters()

    if (params.getParameter("outputPath") != null) {
      return params
    }

    return JobParametersBuilder(params)
      .addString("outputPath", createTempDirectory().pathString)
      .toJobParameters()
  }
}

class NPESkipPolicy : SkipPolicy {
  override fun shouldSkip(t: Throwable, skipCount: Long): Boolean = when (t) {
    is NullPointerException -> {
      NdmisReferralPerformanceReportJobConfiguration.logger.warn("skipping row with unexpected state", t)
      true
    }
    else -> false
  }
}

class CsvLineAggregator<T : Any>(fieldsToExtract: List<String>) : ExtractorLineAggregator<T>() {
  init {
    setFieldExtractor(
      BeanWrapperFieldExtractor<T>().apply {
        setNames(fieldsToExtract.toTypedArray())
      },
    )
  }

  private val csvPrinter = CSVFormat.DEFAULT.builder()
    .setRecordSeparator("") // the underlying aggregator adds line separators for us
    .build()

  override fun doAggregate(fields: Array<out Any>): String {
    val out = StringBuilder()
    csvPrinter.printRecord(out, *fields)
    return out.toString()
  }
}

// logs progress through an NDMIS report step; the step bean is a singleton, so counts are reset for each run in beforeStep
class ReferralChunkProgressListener(private val entityManagerFactory: EntityManagerFactory, private val reportName: String) :
  ChunkListener<Any, Any>,
  StepExecutionListener {
  companion object : KLogging()
  private var totalRecords: Long? = null

  // counted here rather than read from the step execution, which Spring Batch 6 only updates after afterChunk
  private var readCount = 0L

  override fun beforeStep(stepExecution: StepExecution) {
    totalRecords = null
    readCount = 0
  }

  override fun beforeChunk(chunk: Chunk<Any>) {
    readCount += chunk.size()
    if (totalRecords == null) {
      try {
        val em = entityManagerFactory.createEntityManager()
        try {
          totalRecords = when (reportName) {
            "appointment" -> {
              // Count total appointments using native SQL (same logic as the report query)
              em.createNativeQuery(
                """
                SELECT count(*)
                FROM appointment a
                         INNER JOIN referral r ON r.id = a.referral_id
                         LEFT JOIN delivery_session_appointment ds ON a.id = ds.appointment_id
                         LEFT JOIN supplier_assessment_appointment sa ON a.id = sa.appointment_id
                WHERE ds.appointment_id IS NOT NULL OR sa.appointment_id IS NOT NULL
                """.trimIndent(),
              ).singleResult as Long
            }
            "complexity" -> {
              em.createNativeQuery(
                """
                  SELECT count(*)
                  FROM referral r
                  INNER JOIN intervention i ON r.intervention_id = i.id
                  INNER JOIN referral_selected_service_category rssc ON r.id = rssc.referral_id
                  INNER JOIN service_category sc ON rssc.service_category_id = sc.id
                  LEFT JOIN referral_complexity_level_ids rcli ON r.id = rcli.referral_id 
                    AND rcli.complexity_level_ids_key = sc.id
                  LEFT JOIN complexity_level cl ON sc.id = cl.service_category_id 
                    AND cl.id = rcli.complexity_level_ids
                """.trimIndent(),
              ).singleResult as Long
            }
            "outcome" -> {
              em.createNativeQuery(
                """
                  SELECT count(*)
                  FROM referral r
                  INNER JOIN end_of_service_report eosr ON r.id = eosr.referral_id
                  INNER JOIN end_of_service_report_outcome esor ON eosr.id = esor.end_of_service_report_id
                  INNER JOIN desired_outcome deso ON esor.desired_outcome_id = deso.id
                """.trimIndent(),
              ).singleResult as Long
            }
            else -> {
              em.createQuery("select count(r) from Referral r", Long::class.java)
                .singleResult
            }
          }
          logger.info("NDMIS $reportName report: Total records to process = {}", totalRecords)
        } finally {
          em.close()
        }
      } catch (e: Exception) {
        logger.warn("Failed to get total $reportName count", e)
      }
    }
  }

  override fun afterChunk(chunk: Chunk<Any>) {
    val remaining = totalRecords?.let { it - readCount } ?: "unknown"
    logger.info(
      "NDMIS $reportName report: Processed {} records, {} remaining",
      readCount,
      remaining,
    )
  }

  override fun onChunkError(exception: Exception, chunk: Chunk<Any>) {
    val remaining = totalRecords?.let { it - readCount } ?: "unknown"
    logger.warn(
      "NDMIS $reportName report: Error after processing {} records, approximately {} remaining",
      readCount,
      remaining,
    )
  }
}
