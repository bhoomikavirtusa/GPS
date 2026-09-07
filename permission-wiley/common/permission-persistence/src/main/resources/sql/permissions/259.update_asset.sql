-- no need to merge

drop trigger history_asset_u;

update asset set filename = null where length(trim(filename)) = 0;

create trigger history_asset_u after update on asset
for each row begin
  insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
  select distinct au.cw_id, current_timestamp, current_timestamp, ab.last_updated_user_id, ab.created_user_id, 'Asset(s) added/modified'
  from asset a, asset_base ab, asset_use au
    where ab.id = a.id
    and a.id = new.id and au.asset_id = a.id
    and au.cw_id not in (select cw_id from cw_history where cw_id = au.cw_id
      and ab.last_updated_user_id = last_updated_user_id
      -- for some reason description = 'Asset(s) added/modified' does not match so use like
  	  and description like 'Asset% added/modified'
	  and datediff(last_updated_date, current_timestamp) = 0
	 );
end;
