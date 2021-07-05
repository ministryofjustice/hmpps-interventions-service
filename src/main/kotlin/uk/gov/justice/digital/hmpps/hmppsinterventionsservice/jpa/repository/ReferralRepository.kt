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

  @Query(value =
    "WITH attended_sessions AS ( " +
        "select count(app.id) AS attended, min(app.appointment_time) as first_appointment, aps.action_plan_id, act.referral_id " +
            "from appointment app " +
                "join action_plan_session_appointment apsa on app.id = apsa.appointment_id " +
                "join action_plan_session aps on apsa.action_plan_session_id = aps.id " +
                "join action_plan act on aps.action_plan_id = act.id " +
            "where attended in ('YES', 'LATE') " +
            "group by aps.action_plan_id, act.referral_id), " +
        "outcomes_to_be_achieved AS ( " +
            "select count(des.id) as outcomes_to_be_achieved, ref.id AS referral_id " +
                "from referral ref " +
                    "inner join referral_selected_service_category rssc on rssc.referral_id = ref.id " +
                    "inner join desired_outcome des on des.service_category_id = rssc.service_category_id " +
                "group by ref.id), " +
        "achieved_outcomes AS ( " +
            "select sub.partially_achieved + sub.achieved_count AS total, referral_id " +
                "from (select ref.id AS referral_id, " +
                    "sum(case when eosro.achievement_level = 'PARTIALLY_ACHIEVED' then 0.5 end) partially_achieved, " +
                    "sum(case when eosro.achievement_level = 'ACHIEVED' then 1 end) achieved_count " +
                        "from referral ref " +
                            "inner join end_of_service_report eosr on eosr.referral_id = ref.id " +
                            "inner join end_of_service_report_outcome eosro on eosro.end_of_service_report_id = eosr.id " +
                        "group by ref.id) sub), " +
        "supplier_assessment_apointments AS ( " +
            "select app.created_at as appointment_booked_time, " +
            "case when (app.attended in ('LATE', 'YES')) then app.appointment_time else null end as attended_time, " +
            "sup.referral_id " +
                "from appointment app " +
                    "join supplier_assessment_appointment saa on app.id = saa.appointment_id " +
                    "join supplier_assessment sup on saa.supplier_assessment_id = sup.id ) " +
        "select " +
            "null AS referralLink, " +
            "ref.reference_number AS referralReference, " +
            "cast(ref.id as varchar) AS referralId, " +
            "con.contract_reference AS contractId, " +
            "cast(con.prime_provider_id as varchar) AS organisationId, " +
            "null AS referringOfficerEmail, " +
            "null AS caseworkerId, " +
            "ref.service_usercrn AS serviceUserCRN, " +
            "ref.created_at AS dateReferralReceived, " +
            "saa.appointment_booked_time AS dateSAABooked, " +
            "saa.attended_time AS dateSAAAttended, " +
            "cast(act.submitted_at as TIMESTAMP WITH TIME ZONE) AS dateFirstActionPlanSubmitted, " +
            "cast(act.approved_at as TIMESTAMP WITH TIME ZONE) AS dateOfFirstActionPlanApproval, " +
            "cast(ats.first_appointment as TIMESTAMP WITH TIME ZONE) AS dateOfFirstAttendedSession, " +
            "otba.outcomes_to_be_achieved AS outcomesToBeAchievedCount, " +
            "aco.total AS outcomesAchieved, " +
            "act.number_of_sessions AS countOfSessionsExpected, " +
            "ats.attended AS countOfSessionsAttended, " +
            "cast(ref.end_requested_at as TIMESTAMP WITH TIME ZONE) AS endRequestedByPPAt, " +
            "ref.end_requested_by_id AS endRequestedByPPReason, " +
            "eosr.submitted_at AS dateEOSRSubmitted, " +
            "cast(ref.concluded_at as TIMESTAMP WITH TIME ZONE) AS concludedAt " +
                "from referral ref " +
                    "left join attended_sessions ats on ats.referral_id = ref.id " +
                    "left join action_plan act on act.referral_id = ref.id " +
                    "left join outcomes_to_be_achieved otba on otba.referral_id = ref.id " +
                    "JOIN intervention i ON (ref.intervention_id = i.id) " +
                    "JOIN dynamic_framework_contract con ON (i.dynamic_framework_contract_id = con.id) " +
                    "left join end_of_service_report eosr ON (eosr.referral_id = ref.id) " +
                    "left join achieved_outcomes aco ON (aco.referral_id = ref.id) " +
                    "left join supplier_assessment_apointments saa on (saa.referral_id = ref.id) " +
                "where (ref.sent_at >= :from or ref.sent_at is null) " +
                "and (ref.end_requested_at <= :to or ref.end_requested_at is null) " +
                "and con.id in :contracts"
    , nativeQuery = true)
  fun reportingData(to: OffsetDateTime, from: OffsetDateTime, contracts: Set<DynamicFrameworkContract>): List<ReferralReportData>
}
