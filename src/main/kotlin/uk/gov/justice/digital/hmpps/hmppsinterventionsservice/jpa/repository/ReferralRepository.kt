package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProviderSentReferralSummary
import java.time.OffsetDateTime
import java.util.UUID

interface ReferralRepository : JpaRepository<Referral, UUID>, JpaSpecificationExecutor<Referral> {
  @Query(
    value = """select 
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
      cast(r.concluded_at as TIMESTAMP WITH TIME ZONE) as concludedAt,
			row_number() over(partition by r.id order by ra.assigned_at desc) as assigned_at_desc_seq
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
		  and not (
		  	    (r.concluded_At is not null and r.end_Requested_At is not null and eosr.id is null) -- cancelled
	 	        and app.attendance_submitted_at is null -- supplier assessment feedback not submitted
            and ap.submitted_at is null -- action plan has not been submitted
        ) -- filter out referrals that are cancelled with SAA feedback not completed yet or cancelled with no action plan submitted
		and ( dfc.prime_provider_id in :serviceProviders or dfcsc.subcontractor_provider_id in :serviceProviders )
) a where assigned_at_desc_seq = 1""",
    nativeQuery = true
  )
  fun referralSummaryForServiceProviders(serviceProviders: List<String>): List<ServiceProviderSentReferralSummary>

  // queries for sent referrals
  fun findByIdAndSentAtIsNotNull(id: UUID): Referral?
  fun existsByReferenceNumber(reference: String): Boolean

  // queries for draft referrals
  fun findByIdAndSentAtIsNull(id: UUID): Referral?
  fun findByCreatedByIdAndSentAtIsNull(userId: String): List<Referral>

  // queries for reporting
  @Query("select r from Referral r where r.sentAt > :from and r.sentAt < :to and r.intervention.dynamicFrameworkContract in :contracts")
  fun serviceProviderReportReferrals(from: OffsetDateTime, to: OffsetDateTime, contracts: Set<DynamicFrameworkContract>, pageable: Pageable): Page<Referral>
}
