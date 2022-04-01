insert into referral_details (
    id,
    superseded_by_id,
    created_at,
    created_by,
    reason_for_change,
    referral_id,
    completion_deadline,
    further_information,
    maximum_enforceable_days
)
select
    gen_random_uuid(),
    null,
    ref.created_at,
    ref.created_by_id,
    'initial referral details (populated from existing referral)',
    ref.id,
    ref.completion_deadline,
    ref.further_information,
    ref.maximum_enforceable_days
from
    referral ref
        left join referral_details rd on ref.id = rd.referral_id
where rd is null
;