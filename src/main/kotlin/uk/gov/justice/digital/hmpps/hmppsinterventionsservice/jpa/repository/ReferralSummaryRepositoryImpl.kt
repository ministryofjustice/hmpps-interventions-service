package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.dto.DashboardType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.PersonCurrentLocationType
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProviderSentReferralSummary
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.service.ReferralSummaryQuery
import java.sql.Date
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

data class ReferralSummary(
  val referralId: UUID,
  val sentAt: OffsetDateTime,
  val referenceNumber: String,
  val interventionTitle: String,
  val crn: String,
  val dynamicFrameworkContractId: UUID,
  val assignedToUserName: String?,
  val assignedToUserId: String?,
  val assignedToAuthSource: String?,
  val sentByUserName: String,
  val sentByUserId: String,
  val sentByAuthSource: String,
  val serviceUserFirstName: String?,
  val serviceUserLastName: String?,
  val locationType: PersonCurrentLocationType?,
  val prisonId: String? = null,
  val expectedReleaseDate: LocalDate? = null,
  val isReferralReleasingIn12Weeks: Boolean? = null,
  val probationOffice: String? = null,
  val pdu: String? = null,
  val nDeliusPDU: String? = null,
  val concludedAt: OffsetDateTime? = null,
  val serviceProviderId: String,
  val serviceProviderName: String,
)

@Component
class ReferralSummaryRepositoryImpl : ReferralSummaryRepository {

  companion object {
    private val logger = KotlinLogging.logger {}
  }

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private fun summariesQuery(customCriteria: String?): String {
    val dashboardRestrictionCriteria = customCriteria?.let { " $customCriteria " } ?: ""
    return """select 
      referralId, 
      sentAt,
			referenceNumber,
			interventionTitle,
      dynamicFrameworkContractId,
			assignedToUserName,
			serviceUserFirstName,
			serviceUserLastName,
      endOfServiceReportId,
      endOfServiceReportSubmittedAt,
      concludedAt from (	
	select
			cast(r.id as varchar) AS referralId,
			cast(r.sent_at as TIMESTAMP WITH TIME ZONE) as sentAt,
			r.reference_number as referenceNumber,
      cast(dfc.id as varchar) as dynamicFrameworkContractId,
			au.user_name as assignedToUserName,
			i.title as interventionTitle,
			rsud.first_name as serviceUserFirstName,
			rsud.last_name as serviceUserLastName,
      cast(eosr.id as varchar) as endOfServiceReportId,
      cast(eosr.submitted_at as TIMESTAMP WITH TIME ZONE) as endOfServiceReportSubmittedAt,
			row_number() over(partition by r.id order by ra.assigned_at desc) as assigned_at_desc_seq,
      cast(r.concluded_at as TIMESTAMP WITH TIME ZONE) as concludedAt
	from referral r
			 inner join intervention i on i.id = r.intervention_id
			 left join referral_service_user_data rsud on rsud.referral_id = r.id
			 inner join dynamic_framework_contract dfc on dfc.id = i.dynamic_framework_contract_id
			 left join dynamic_framework_contract_sub_contractor dfcsc on dfcsc.dynamic_framework_contract_id = dfc.id
			 left join referral_assignments ra on ra.referral_id = r.id
			 left join auth_user au on au.id = ra.assigned_to_id
			 left join end_of_service_report eosr on eosr.referral_id = r.id
			 left outer join action_plan ap on ap.referral_id = r.id
	 	 	 left outer join appointment app
       	 left join supplier_assessment_appointment saa on saa.appointment_id = app.id
       	 on app.referral_id = r.id
	where
		  r.sent_at is not null
		  and ( dfc.prime_provider_id in :serviceProviders or dfcsc.subcontractor_provider_id in :serviceProviders )
      and not (
		  	    (r.concluded_At is not null and r.end_Requested_At is not null and eosr.id is null) -- cancelled
	 	        and app.attendance_submitted_at is null -- supplier assessment feedback not submitted
            and ap.submitted_at is null -- action plan has not been submitted
        ) -- filter out referrals that are cancelled with SAA feedback not completed yet or cancelled with no action plan submitted
) a where assigned_at_desc_seq = 1 $dashboardRestrictionCriteria"""
  }

  private fun referralSummaryQuery(customCriteria: String?): String {
    val dashboardRestrictionCriteria = customCriteria?.let { " $customCriteria " } ?: ""
    return """
      SELECT referralid,
       sentat,
       referencenumber,
       interventiontitle,
       crn,
       dynamicframeworkcontractid,
       assignedtousername,
       assignedtouserid,
       assignedtoauthsource,
       sentbyusername,
       sentbyuserid,
       sentbyauthsource,
       serviceuserfirstname,
       serviceuserlastname,
       locationtype,
       prisonid,
       expectedreleasedate,
       referralreleasingin12weeks,
       probationoffice,
       pdu,
       ndeliuspdu,
       concludedat,
       serviceproviderid,
       serviceprovidername
FROM   (SELECT r.id                              AS referralId,
               r.sent_at                         AS sentAt,
               r.reference_number                AS referenceNumber,
               r.service_usercrn                 AS crn,
               dfc.id                            AS dynamicFrameworkContractId,
               au.user_name                      AS assignedToUserName,
               au.id                             AS assignedToUserId,
               au.auth_source                    AS assignedToAuthSource,
               sb.user_name                      AS sentByUserName,
               sb.id                             AS sentByUserId,
               sb.auth_source                    AS sentByAuthSource,
               i.title                           AS interventionTitle,
               rsud.first_name                   AS serviceUserFirstName,
               rsud.last_name                    AS serviceUserLastName,
               Row_number()
                 over(
                   PARTITION BY r.id
                   ORDER BY ra.assigned_at DESC) AS assigned_at_desc_seq,
               loc.TYPE                          AS locationType,
               loc.prison_id                     AS prisonId,
               loc.expected_release_date         AS expectedReleaseDate,
               loc.referral_releasing_12_weeks   AS referralReleasingIn12Weeks,
               pd.probation_office               AS probationOffice,
               pd.pdu                            AS pdu,
               pd.ndelius_pdu                    AS nDeliusPDU,
               r.concluded_at                    AS concludedAt,
               sp.id                             AS serviceProviderId,
               sp.name                           AS serviceProviderName
        FROM   referral r
               inner join intervention i
                       ON i.id = r.intervention_id
               left join referral_service_user_data rsud
                      ON rsud.referral_id = r.id
               inner join dynamic_framework_contract dfc
                       ON dfc.id = i.dynamic_framework_contract_id
               left join dynamic_framework_contract_sub_contractor dfcsc
                      ON dfcsc.dynamic_framework_contract_id = dfc.id
               inner join service_provider sp
                      ON dfc.prime_provider_id = sp.id
               left join referral_assignments ra
                      ON ra.referral_id = r.id
                         AND ra.superseded = FALSE
               left join auth_user au
                      ON au.id = ra.assigned_to_id
               left join auth_user sb
                      ON sb.id = r.sent_by_id
               left join referral_location loc
                      ON loc.referral_id = r.id
               left join probation_practitioner_details pd
                      ON pd.referral_id = r.id
               left join end_of_service_report eosr
                      ON eosr.referral_id = r.id
        WHERE  r.sent_at IS NOT NULL $dashboardRestrictionCriteria
        ) a
WHERE  assigned_at_desc_seq = 1
    """.trimIndent()
  }

  override fun getSentReferralSummaries(authUser: AuthUser, serviceProviders: List<String>, dashboardType: DashboardType?): List<ServiceProviderSentReferralSummary> {
    val query = entityManager.createNativeQuery(summariesQuery(constructCustomCriteria(dashboardType)))
    query.setParameter("serviceProviders", serviceProviders)
    if (dashboardType == DashboardType.MyCases) {
      query.setParameter("username", authUser.userName)
    }
    val result = query.resultList as List<Array<Any>>
    val summaries: MutableList<ServiceProviderSentReferralSummary> = mutableListOf()
    result.forEach { row ->
      val referralId = row[0] as String
      val sentAt = row[1] as Instant
      val referenceNumber = row[2] as String
      val interventionTitle = row[3] as String
      val dynamicFrameWorkContractId = row[4] as String
      val assignedToUserName = row[5] as String?
      val serviceUserFirstName = row[6] as String?
      val serviceUserLastName = row[7] as String?
      val endOfServiceReportId = (row[8] as String?)?.let { UUID.fromString(it) }
      val endOfServiceReportSubmittedAt = row[9] as Instant?
      summaries.add(ServiceProviderSentReferralSummary(referralId, sentAt, referenceNumber, interventionTitle, dynamicFrameWorkContractId, assignedToUserName, serviceUserFirstName, serviceUserLastName, endOfServiceReportId, endOfServiceReportSubmittedAt))
    }
    return summaries
  }

  override fun getReferralSummaries(
    referralSummaryQuery: ReferralSummaryQuery,
  ): Page<ReferralSummary> {
    val (
      concluded,
      cancelled,
      unassigned,
      assignedToUserId,
      page,
      searchText,
      isSpUser,
      isPPUser,
      createdById,
      serviceUserCrns,
      serviceProviders,
    ) = referralSummaryQuery

    val queryBuilder = StringBuilder(referralSummaryQuery(constructCustomCriteria(concluded, cancelled, unassigned, assignedToUserId, searchText, isSpUser, isPPUser)))

    // Append ORDER BY clause if sorting is specified
    if (page.sort.isSorted) {
      queryBuilder.append(" ORDER BY ")
      page.sort.forEach { order ->
        // Assuming the sort property is in the format "table.column"
        val column = order.property
        val direction = order.direction
        queryBuilder.append("$column $direction")
        queryBuilder.append(", ")
      }
      // Remove the trailing comma and space
      queryBuilder.setLength(queryBuilder.length - 2)
    }

    val fullQuery = queryBuilder.toString()
    logger.debug("The dashboard query is:  $fullQuery")

    // Create the native query
    val query = entityManager.createNativeQuery(fullQuery)

    // Set pagination parameters
    query.setFirstResult(page.pageNumber * page.pageSize)
    query.setMaxResults(page.pageSize)

    // Set any parameters if needed
    searchText?.let { query.setParameter("searchText", searchText.uppercase()) }
    isPPUser.let {
      if (it) {
        query.setParameter("createdById", createdById)
        query.setParameter("serviceUserCrns", serviceUserCrns)
        assignedToUserId?.let { query.setParameter("assignedToUserId", assignedToUserId) }
      }
    }
    isSpUser.let {
      if (it) {
        assignedToUserId?.let { query.setParameter("assignedToUserId", assignedToUserId) }
        query.setParameter("serviceProviders", serviceProviders)
      }
    }

    // Execute the query
    val results = query.resultList as List<Array<Any>>
    val referralSummaries: MutableList<ReferralSummary> = mutableListOf()
    results.forEach { row ->
      val referralId = row[0] as UUID
      val sentAt = instantToOffsetNotNull(row[1] as Instant)
      val referenceNumber = row[2] as String
      val interventionTitle = row[3] as String
      val crn = row[4] as String
      val dynamicFrameWorkContractId = row[5] as UUID
      val assignedToUserName = row[6] as String?
      val assignedToUserId = row[7] as String?
      val assignedToAuthSource = row[8] as String?
      val sentByUserName = row[9] as String
      val sentByUserId = row[10] as String
      val sentByAuthSource = row[11] as String
      val serviceUserFirstName = row[12] as String?
      val serviceUserLastName = row[13] as String?
      val locationType = (row[14] as String?)?.let { PersonCurrentLocationType.valueOf(it) }
      val prisonId = row[15] as String?
      val expectedReleaseDate = (row[16] as Date?)?.toLocalDate()
      val isReferralReleasingIn12Weeks = row[17] as Boolean?
      val probationOffice = row[18] as String?
      val pdu = row[19] as String?
      val nDeliusPDU = row[20] as String?
      val concludedAt = instantToOffsetNull(row[21] as Instant?)
      val serviceProviderId = row[22] as String
      val serviceProviderName = row[23] as String
      referralSummaries.add(
        ReferralSummary(
          referralId,
          sentAt,
          referenceNumber,
          interventionTitle,
          crn,
          dynamicFrameWorkContractId,
          assignedToUserName,
          assignedToUserId,
          assignedToAuthSource,
          sentByUserName,
          sentByUserId,
          sentByAuthSource,
          serviceUserFirstName,
          serviceUserLastName,
          locationType,
          prisonId,
          expectedReleaseDate,
          isReferralReleasingIn12Weeks,
          probationOffice,
          pdu,
          nDeliusPDU,
          concludedAt,
          serviceProviderId,
          serviceProviderName,
        ),
      )
    }

    val totalCount = totalCount(
      concluded,
      cancelled,
      unassigned,
      assignedToUserId,
      searchText,
      isSpUser,
      isPPUser,
      createdById,
      serviceUserCrns,
      serviceProviders,
    )
    // Create Pageable object
    val pageable = PageRequest.of(page.pageNumber, page.pageSize, page.sort ?: Sort.unsorted())

    // Return Page object
    return PageImpl(referralSummaries, pageable, totalCount)
  }

  private fun totalCount(
    concluded: Boolean?,
    cancelled: Boolean?,
    unassigned: Boolean?,
    assignedToUserId: String?,
    searchText: String?,
    isSpUser: Boolean,
    isPPUser: Boolean,
    createdById: String?,
    serviceUserCrns: List<String>?,
    serviceProviders: List<String>?,
  ): Long {
    // Get total count (you need a separate count query for this)
    val countQuery = "SELECT COUNT(*) FROM (${
      referralSummaryQuery(
        constructCustomCriteria(
          concluded,
          cancelled,
          unassigned,
          assignedToUserId,
          searchText,
          isSpUser,
          isPPUser,
        ),
      )
    }) as count_query"
    val countQueryObj = entityManager.createNativeQuery(countQuery)

    // Set parameters for count query if needed
    searchText?.let { countQueryObj.setParameter("searchText", searchText.uppercase()) }
    isPPUser.let {
      if (it) {
        countQueryObj.setParameter("createdById", createdById)
        countQueryObj.setParameter("serviceUserCrns", serviceUserCrns)
        assignedToUserId?.let { countQueryObj.setParameter("assignedToUserId", assignedToUserId) }
      }
    }
    isSpUser.let {
      if (it) {
        assignedToUserId?.let { countQueryObj.setParameter("assignedToUserId", assignedToUserId) }
        countQueryObj.setParameter("serviceProviders", serviceProviders)
      }
    }

    val totalCount = (countQueryObj.singleResult as Number).toLong()
    return totalCount
  }

  private fun constructCustomCriteria(
    concluded: Boolean?,
    cancelled: Boolean?,
    unassigned: Boolean?,
    assignedToUserId: String?,
    searchText: String?,
    isSpUser: Boolean,
    isPpUser: Boolean,
  ): String {
    val customCriteria = StringBuilder()
    concluded?.let { if (it) customCriteria.append("and r.concluded_at  is not null ") else customCriteria.append("and r.concluded_at  is null ") }
    cancelled?.let { if (it) customCriteria.append("and (r.concluded_at is not null and  r.end_requested_at is not null and eosr.id is null) ") else customCriteria.append("and not (r.concluded_at is not null and r.end_requested_at is not null and eosr.id is null) ") }
    unassigned?.let { if (it) customCriteria.append("and ra.assigned_to_id is null ") else customCriteria.append("and not (ra.assigned_to_id is null) ") }
    assignedToUserId?.let { customCriteria.append("and au.id = :assignedToUserId ") }
    searchText?.let { customCriteria.append(searchQuery(it)) }
    isSpUser.let { if (it) customCriteria.append(constructSPQuery()) }
    isPpUser.let { if (it) customCriteria.append(constructPPQuery()) }
    return customCriteria.toString()
  }

  private fun constructPPQuery(): String {
    return "and (r.created_by_id = :createdById or r.service_usercrn in :serviceUserCrns) "
  }

  private fun constructSPQuery(): String {
    return "and ( dfc.prime_provider_id in :serviceProviders or dfcsc.subcontractor_provider_id in :serviceProviders ) "
  }

  private fun searchQuery(searchText: String): String {
    return if (searchText.matches(Regex("[A-Z]{2}[0-9]{4}[A-Z]{2}"))) {
      "and r.reference_number= :searchText "
    } else {
      "and concat(UPPER(rsud.first_name), ' ', UPPER(rsud.last_name)) = :searchText "
    }
  }

  private fun constructCustomCriteria(dashboardType: DashboardType?): String? {
    return when (dashboardType) {
      DashboardType.MyCases -> "and assignedToUserName = :username and concludedAt is null "
      DashboardType.OpenCases -> "and concludedAt is null "
      DashboardType.UnassignedCases -> "and assignedToUserName is null and concludedAt is null"
      DashboardType.CompletedCases -> "and concludedAt is not null "
      null -> null
    }
  }

  private fun instantToOffsetNotNull(instant: Instant?): OffsetDateTime {
    val resolved = instant ?: Instant.now()
    return OffsetDateTime.ofInstant(resolved, ZoneId.systemDefault())
  }

  private fun instantToOffsetNull(instant: Instant?): OffsetDateTime? {
    val resolved = instant ?: Instant.now()
    return instant ?.let { OffsetDateTime.ofInstant(resolved, ZoneId.systemDefault()) }
  }
}
