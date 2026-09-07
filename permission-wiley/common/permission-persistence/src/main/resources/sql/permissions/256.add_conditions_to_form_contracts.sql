-- no need to merge with seed or create

delete from contract_2_condition where contract_id in (select id from contract where is_permission_form = 1);

-- then run ConvertConditions5

-- then recalc status where status is amendmentNeeded (use DevMisc page and URL on there)
