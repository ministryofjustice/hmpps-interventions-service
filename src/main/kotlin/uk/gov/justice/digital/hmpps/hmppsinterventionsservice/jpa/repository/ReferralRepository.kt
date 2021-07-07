package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.AuthUser
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.DynamicFrameworkContract
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.Referral
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ReferralReportData
import uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity.ServiceProvider
import java.time.OffsetDateTime
import java.util.*

interface ReferralRepository : JpaRepository<Referral, UUID> {
  // queries for service providers
  fun findAllByInterventionDynamicFrameworkContractPrimeProviderInAndSentAtIsNotNull(providers: Iterable<ServiceProvider>): List<Referral>
  fun findAllByInterventionDynamicFrameworkContractSubcontractorProvidersInAndSentAtIsNotNull(providers: Iterable<ServiceProvider>): List<Referral>

  // queries for sent referrals
  fun findByIdAndSentAtIsNotNull(id: UUID): Referral?
  fun findByCreatedByAndSentAtIsNotNull(user: AuthUser): List<Referral>
  fun existsByReferenceNumber(reference: String): Boolean
  fun findByServiceUserCRNAndSentAtIsNotNull(crn: String): List<Referral>

  // queries for draft referrals
  fun findByIdAndSentAtIsNull(id: UUID): Referral?
  fun findByCreatedByIdAndSentAtIsNull(userId: String): List<Referral>

  @Query(
    value =
    """with 
action_plans_dates as (
  	select referral_id, id action_plan_id, number_of_sessions, submitted_at, approved_at,
		       row_number() over(partition by referral_id order by approved_at asc) as approved_at_asc_seq,
           row_number() over(partition by referral_id order by submitted_at asc) as submitted_at_asc_seq,   
 		       row_number() over(partition by referral_id order by created_at desc) as created_at_desc_seq
	  from action_plan
),
attended_sessions as (
    select aps.action_plan_id, count(app.id) as attended, 
           min(app.appointment_time) as first_appointment_time
	  from action_plan_session aps 
    left join action_plan_session_appointment apsa on apsa.action_plan_session_id = aps.id
    left join appointment app on app.id = apsa.appointment_id
    where attended in ('YES', 'LATE')
    group by aps.action_plan_id
),
outcomes_to_be_achieved as (
    select referral_id, count(service_category_id) as outcomes_count
    from referral_selected_service_category
    group by referral_id
),
achieved_outcomes as (
	  select end_of_service_report_id,
           sum(case when achievement_level = 'PARTIALLY_ACHIEVED' then 0.5
                    when achievement_level = 'ACHIEVED' then 1 
                    else 0 end) achieved_count
    from end_of_service_report_outcome
    group by end_of_service_report_id
)
select
      null AS referralLink,
      r.reference_number AS referralReference,
      cast(r.id as varchar) AS referralId,
      dfc.contract_reference AS contractId,
      cast(dfc.prime_provider_id as varchar) AS organisationId,
      null AS referringOfficerEmail,
      null AS caseworkerId,
      r.service_usercrn AS serviceUserCRN,
      r.created_at AS dateReferralReceived,
      sa_app.created_at AS dateSAABooked, 
      case when (sa_app.attended in ('LATE', 'YES')) then sa_app.appointment_time else null end dateSAAAttended,
      cast(apd_sa.submitted_at as TIMESTAMP WITH TIME ZONE) AS dateFirstActionPlanSubmitted,
      cast(apd_aa.approved_at as TIMESTAMP WITH TIME ZONE) AS dateOfFirstActionPlanApproval,
      cast(asess.first_appointment_time as TIMESTAMP WITH TIME ZONE) AS dateOfFirstAttendedSession,
      otba.outcomes_count AS outcomesToBeAchievedCount,
      oa.achieved_count AS outcomesAchieved,
      apd_aa.number_of_sessions AS countOfSessionsExpected,
      asess.attended AS countOfSessionsAttended,
      cast(r.end_requested_at as TIMESTAMP WITH TIME ZONE) AS endRequestedByPPAt,
      cr.description AS endRequestedByPPReason,
      eosr.submitted_at AS dateEOSRSubmitted,
      cast(r.concluded_at as TIMESTAMP WITH TIME ZONE) AS concludedAt
from referral r
inner join intervention i on i.id = r.intervention_id
inner join dynamic_framework_contract dfc on dfc.id = i.dynamic_framework_contract_id
left join outcomes_to_be_achieved otba on otba.referral_id = r.id
left join supplier_assessment sa on sa.referral_id = r.id
left join supplier_assessment_appointment saa on saa.supplier_assessment_id = sa.id
left join appointment sa_app on sa_app.id = saa.appointment_id
left join end_of_service_report eosr on eosr.referral_id = r.id 
left join cancellation_reason cr on cr.code = r.end_requested_reason_code
left join action_plans_dates apd_sa on apd_sa.referral_id = r.id 
                                   and apd_sa.submitted_at_asc_seq = 1 
left join action_plans_dates apd_aa on apd_aa.referral_id = r.id 
                                   and apd_aa.approved_at_asc_seq = 1 
left join action_plans_dates apd_ca on apd_ca.referral_id = r.id 
                                   and apd_ca.created_at_desc_seq = 1
left join attended_sessions asess on asess.action_plan_id = apd_ca.action_plan_id
left join achieved_outcomes oa on oa.end_of_service_report_id = eosr.id
where (r.end_requested_at >= :from or r.end_requested_at is null)
and (r.sent_at <= :to)
and dfc.id in :contracts""",
    nativeQuery = true
  )
  fun reportingData(from: OffsetDateTime, to: OffsetDateTime, contracts: Set<DynamicFrameworkContract>): List<ReferralReportData>
}
