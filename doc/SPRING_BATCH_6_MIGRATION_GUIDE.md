# Spring Batch 6.0 Migration Guide for HMPPS Interventions Service

> **Correction (October 7, 2026)**: `ListItemReader` and `DefaultJobParametersValidator` were **not removed** in Spring Batch 6 – they moved to
> `org.springframework.batch.infrastructure.item.support.ListItemReader` and
> `org.springframework.batch.core.job.parameters.DefaultJobParametersValidator`. The codebase now uses them directly;
> the custom replacements described below were removed. `JobParameters.getString(key)` throws if the parameter is not a String,
> so use `getParameter(key) != null` to check for presence.

## RESEARCH FINDINGS

### 1. ListItemReader - REMOVED in Spring Batch 6.0

**Status**: REMOVED - No longer exists in Spring Batch 6.0+

**Current Usage in Project:**
- `PerformanceReportJobConfiguration.kt` line 48
- Used to wrap a List<ReferralPerformanceReport> from a repository

**Replacement Approach:**

The `ListItemReader` was a simple reader that just iterated through a list. In Spring Batch 6.0, there are several recommended approaches:

**Option A: Use the new ItemReaderFactory pattern (RECOMMENDED)**
```kotlin
// Create a simple custom ItemReader if list is small and fits in memory
class ListItemReader<T>(private val items: List<T>) : ItemReader<T> {
    private val iterator = items.iterator()
    
    override fun read(): T? = if (iterator.hasNext()) iterator.next() else null
}
```

**Option B: Implement ItemReader directly (RECOMMENDED)**
```kotlin
@StepScope
@Component
class ReferralPerformanceReportItemReader(
    private val referralPerformanceReportRepository: ReferralPerformanceReportRepository,
    @Value("#{jobParameters['contractReferences']}") private val contractReferences: String,
    @Value("#{jobParameters['from']}") private val from: Date,
    @Value("#{jobParameters['to']}") private val to: Date,
    private val batchUtils: BatchUtils,
): ItemReader<ReferralPerformanceReport> {
    
    private var iterator: Iterator<ReferralPerformanceReport>? = null
    
    override fun read(): ReferralPerformanceReport? {
        if (iterator == null) {
            val items = referralPerformanceReportRepository.serviceProviderReportReferrals(
                batchUtils.parseDateToOffsetDateTime(from),
                batchUtils.parseDateToOffsetDateTime(to),
                contractReferences,
            )
            iterator = items.iterator()
        }
        
        return if (iterator!!.hasNext()) iterator!!.next() else null
    }
}
```

**Option C: Use delegation pattern (if you want to keep ListItemReader utility)**
```kotlin
// Create a custom utility reader in your batch utilities
fun <T> listItemReader(items: List<T>): ItemReader<T> {
    return object : ItemReader<T> {
        private val iterator = items.iterator()
        
        override fun read(): T? = if (iterator.hasNext()) iterator.next() else null
    }
}
```

---

### 2. DefaultJobParametersValidator - REMOVED in Spring Batch 6.0

**Status**: REMOVED - No longer exists in Spring Batch 6.0+

**Current Usage in Project:**
- `PerformanceReportJobConfiguration.kt` line 70-81
- `NdmisReferralPerformanceReportJobConfiguration.kt` line 115-116
- `NdmisComplexityPerformanceReportJobConfiguration.kt` (similar usage)
- `NdmisAppointmentPerformanceReportJobConfiguration.kt` (similar usage)
- `NdmisOutcomePerformanceReportJobConfiguration.kt` (similar usage)
- `TransferReferralsJobConfiguration.kt` line 30-37

**Replacement Approach:**

In Spring Batch 6.0, parameter validation is done through the `Job.getJobParametersValidator()` mechanism, but the `DefaultJobParametersValidator` class has been removed. 

**Recommended Solution: Implement JobParametersValidator interface directly**

```kotlin
import org.springframework.batch.core.job.JobParametersValidator
import org.springframework.batch.core.job.JobParametersInvalidException
import org.springframework.batch.core.job.JobParameters

class CustomJobParametersValidator(private val requiredKeys: Array<String>) : JobParametersValidator {
    override fun validate(parameters: JobParameters?) {
        if (parameters == null) {
            throw JobParametersInvalidException("Job parameters cannot be null")
        }
        
        val missingKeys = requiredKeys.filter { key ->
            !parameters.parameters.containsKey(key)
        }
        
        if (missingKeys.isNotEmpty()) {
            throw JobParametersInvalidException(
                "Missing required job parameters: ${missingKeys.joinToString(", ")}"
            )
        }
    }
}
```

**Usage in Job Configuration:**
```kotlin
@Bean
fun performanceReportJob(writeToCsvStep: Step): Job {
    return JobBuilder("performanceReportJob", jobRepository)
        .validator(CustomJobParametersValidator(arrayOf(
            "contractReferences",
            "user.id",
            "user.firstName",
            "user.email",
            "from",
            "to",
            "timestamp",
        )))
        .listener(listener)
        .start(writeToCsvStep)
        .build()
}
```

**IMPORTANT: JobParameters API Changed in Spring Batch 6.0**

In Spring Batch 6.0, `JobParameters` API changed. Access parameters differently:

```kotlin
// OLD (Spring Batch 5.x) - NO LONGER WORKS
val value = jobParameters.getString("key")
val date = jobParameters.getDate("key")

// NEW (Spring Batch 6.0+) - Use the parameters map
val value = jobParameters.parameters["key"]?.value.toString()

// Or using the correct API:
// JobParameters now uses a map-based approach
for ((key, param) in jobParameters.parameters) {
    println("$key: ${param.value}")
}
```

---

### 3. FlatFileItemWriter - STILL EXISTS ✓

**Status**: STILL AVAILABLE and recommended

**Current Usage in Project:**
- `PerformanceReportJobConfiguration.kt` line 61
- `NdmisReferralPerformanceReportJobConfiguration.kt` line 103
- Used via `BatchUtils.csvFileWriter()` helper
- All NDMIS report jobs use it

**No changes needed** - This API remains stable in Spring Batch 6.0.

The builder pattern usage is the recommended approach:
```kotlin
FlatFileItemWriterBuilder<T>()
    .name("writer")
    .resource(resource)
    .delimited()
    .names(*fields)
    .build()
```

---

### 4. Job/Step Parameter Access - API CHANGES

**Status**: CHANGED - Access patterns updated in Spring Batch 6.0

**Old Pattern (Spring Batch 5.x) - DEPRECATED:**
```kotlin
@Value("#{jobParameters['contractReferences']}") contractReferences: String
@Value("#{jobExecutionContext['output.file.path']}") path: String

// In code:
jobParameters.getString("key")
jobExecution.exitStatus.exitDescription
jobExecution.jobInstance.id
jobExecution.jobParameters.getString("key")
jobExecution.executionContext.put("key", value)
```

**New Pattern (Spring Batch 6.0+) - RECOMMENDED:**

For parameter access, the spelEx syntax still works with @Value, BUT you should understand the new JobParameters API:

```kotlin
// Parameters can be accessed via the map
val params = jobExecution.jobParameters
params.parameters["key"]?.value  // Gets Object value

// Or if you need type-safe access, check parameter type:
val param = params.parameters["contractReferences"]
val value = param?.value.toString()

// The @Value SpEL still works but internally uses this mechanism:
@Value("#{jobParameters['contractReferences']}") contractReferences: String
```

**ExecutionContext (No changes):**
```kotlin
// This API remains the same in 6.0
jobExecution.executionContext.put("key", value)
jobExecution.executionContext.getString("key")
jobExecution.executionContext.get("key")
```

**ExitStatus (No changes):**
```kotlin
// This API remains stable
jobExecution.exitStatus.exitDescription
jobExecution.exitStatus.exitCode
jobExecution.status  // BatchStatus enum
```

**JobInstance (No changes):**
```kotlin
// This API remains stable
jobExecution.jobInstance.id
jobExecution.jobInstance.instanceId
jobExecution.jobInstance.jobName
```

---

## SUMMARY OF REQUIRED CHANGES

| API | Status | Action Required |
|-----|--------|-----------------|
| `ListItemReader` | REMOVED | Create custom ItemReader or utility class |
| `DefaultJobParametersValidator` | REMOVED | Implement `JobParametersValidator` interface |
| `FlatFileItemWriter` | ✓ STABLE | No changes needed |
| Job/Step Parameter Access | ✓ STABLE* | Minor API changes but @Value SpEL still works |
| `JobExecution.executionContext` | ✓ STABLE | No changes |
| `JobExecution.exitStatus` | ✓ STABLE | No changes |
| `JobExecution.jobInstance` | ✓ STABLE | No changes |

*JobParameters.getString() is removed, but SpEL @Value injection still works

---

## AFFECTED FILES IN PROJECT

1. **PerformanceReportJobConfiguration.kt**
   - Line 13: `import org.springframework.batch.item.support.ListItemReader` → CREATE CUSTOM CLASS
   - Line 48: Return type `ListItemReader<ReferralPerformanceReport>` → CHANGE TO ItemReader
   - Line 50: `ListItemReader<ReferralPerformanceReport>(...)` → USE CUSTOM IMPLEMENTATION
   - Line 7: `import org.springframework.batch.core.job.DefaultJobParametersValidator` → REPLACE
   - Line 70: `DefaultJobParametersValidator()` → USE CUSTOM CLASS

2. **NdmisReferralPerformanceReportJobConfiguration.kt**
   - Line 10: `import org.springframework.batch.core.job.DefaultJobParametersValidator` → REPLACE
   - Line 115: `DefaultJobParametersValidator()` → USE CUSTOM CLASS

3. **NdmisComplexityPerformanceReportJobConfiguration.kt**
   - Similar to NdmisReferralPerformanceReportJobConfiguration.kt

4. **NdmisAppointmentPerformanceReportJobConfiguration.kt**
   - Similar to NdmisReferralPerformanceReportJobConfiguration.kt

5. **NdmisOutcomePerformanceReportJobConfiguration.kt**
   - Similar to NdmisReferralPerformanceReportJobConfiguration.kt

6. **TransferReferralsJobConfiguration.kt**
   - Line 5: `import org.springframework.batch.core.job.DefaultJobParametersValidator` → REPLACE
   - Line 30: `DefaultJobParametersValidator()` → USE CUSTOM CLASS

---

## IMPLEMENTATION STEPS

### Step 1: Create CustomJobParametersValidator.kt
Create a reusable validator class in `src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsinterventionsservice/reporting/`:

```kotlin
import org.springframework.batch.core.job.JobParametersValidator
import org.springframework.batch.core.job.JobParametersInvalidException
import org.springframework.batch.core.job.JobParameters

class CustomJobParametersValidator(private val requiredKeys: Array<String>) : JobParametersValidator {
    override fun validate(parameters: JobParameters?) {
        if (parameters == null) {
            throw JobParametersInvalidException("Job parameters cannot be null")
        }
        
        val missingKeys = requiredKeys.filter { key ->
            !parameters.parameters.containsKey(key)
        }
        
        if (missingKeys.isNotEmpty()) {
            throw JobParametersInvalidException(
                "Missing required job parameters: ${missingKeys.joinToString(", ")}"
            )
        }
    }
}
```

### Step 2: Create ListItemReader utility
Add to `BatchUtils.kt`:

```kotlin
fun <T> listItemReader(items: List<T>): ItemReader<T> {
    return object : ItemReader<T> {
        private val iterator = items.iterator()
        
        override fun read(): T? = if (iterator.hasNext()) iterator.next() else null
    }
}
```

### Step 3: Update each Job Configuration
Replace imports and usage:
- Remove: `import org.springframework.batch.item.support.ListItemReader`
- Remove: `import org.springframework.batch.core.job.DefaultJobParametersValidator`
- Add: `import org.springframework.batch.item.ItemReader` (if not present)
- Add: `import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.CustomJobParametersValidator`
- Add: `import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.reporting.listItemReader`

### Step 4: Update reader bean
```kotlin
// OLD
fun reader(...): ListItemReader<ReferralPerformanceReport> {
    return ListItemReader<ReferralPerformanceReport>(items)
}

// NEW
fun reader(...): ItemReader<ReferralPerformanceReport> {
    return listItemReader(items)
}
```

### Step 5: Update validator
```kotlin
// OLD
val validator = DefaultJobParametersValidator()
validator.setRequiredKeys(arrayOf("key1", "key2"))

// NEW
val validator = CustomJobParametersValidator(arrayOf("key1", "key2"))
```

---

## TESTING CHECKLIST

- [ ] Compilation succeeds without deprecation warnings
- [ ] Unit tests pass for all batch configurations
- [ ] Integration tests verify:
  - [ ] Job parameters are properly validated
  - [ ] ListItemReader replacement handles all items
  - [ ] CSV output files are generated correctly
  - [ ] Job execution context values are accessible
  - [ ] Exit status reporting works correctly
  - [ ] Job instance ID tracking works
- [ ] Run actual batch jobs end-to-end with valid parameters
- [ ] Run batch jobs with invalid/missing parameters to test validation

