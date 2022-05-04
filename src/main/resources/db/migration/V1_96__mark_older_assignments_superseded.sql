-- We should have an invariant for assignments that all older assignments have been superseded
-- In other words: there is only ZERO or ONE assignment with "superseded = FALSE"

-- This query fixes the old data not conforming to this rule:
-- |referral_id                         |assigned_at                      |superseded|
-- {snip}
-- |e4ad07b5-7535-4836-9ab8-000000000000|2021-07-28 11:56:36.844071 +00:00|false     |
-- |e4ad07b5-7535-4836-9ab8-000000000000|2021-07-29 14:21:01.560241 +00:00|false     |
-- |e4ad07b5-7535-4836-9ab8-000000000000|2021-08-05 14:41:47.612426 +00:00|false     |

UPDATE referral_assignments oa
SET superseded = TRUE
FROM referral_assignments na
WHERE oa.referral_id = na.referral_id
  AND oa.assigned_at < na.assigned_at
  AND oa.superseded = FALSE;

-- Result:
-- |referral_id                         |assigned_at                      |superseded|
-- {snip}
-- |e4ad07b5-7535-4836-9ab8-000000000000|2021-07-28 11:56:36.844071 +00:00|true      |
-- |e4ad07b5-7535-4836-9ab8-000000000000|2021-07-29 14:21:01.560241 +00:00|true      |
-- |e4ad07b5-7535-4836-9ab8-000000000000|2021-08-05 14:41:47.612426 +00:00|false     |
