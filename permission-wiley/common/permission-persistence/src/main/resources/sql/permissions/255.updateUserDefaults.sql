-- no need to merge with create or seed

-- remove all user custom filters (pivotal 68680492)
update user_defaults set custom_filter = null;