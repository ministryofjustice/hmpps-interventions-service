create or replace function performance_report_eosr_outcome_score(eosr_id UUID)
  returns decimal
as
$body$
select sum(cast(points AS decimal(5,1))) as score from (
select 1 points from end_of_service_report eosr
join end_of_service_report_outcome eosro on eosr.id = eosro.end_of_service_report_id
where eosro.end_of_service_report_id = $1
and
 eosro.achievement_level = 'ACHIEVED'
and
eosr.submitted_at is not null
union
select 0.5 points from end_of_service_report eosr
join end_of_service_report_outcome eosro on eosr.id = eosro.end_of_service_report_id
where eosro.end_of_service_report_id = $1
and
 eosro.achievement_level = 'PARTIALLY_ACHIEVED'
and
eosr.submitted_at is not null
union
select 0 points from end_of_service_report eosr
join end_of_service_report_outcome eosro on eosr.id = eosro.end_of_service_report_id
where eosro.end_of_service_report_id = $1
and
 eosro.achievement_level = 'NOT_ACHIEVED'
and
eosr.submitted_at is not null) as score_elements
$body$
language sql;