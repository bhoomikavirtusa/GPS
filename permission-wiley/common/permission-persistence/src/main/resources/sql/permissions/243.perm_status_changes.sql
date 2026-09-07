-- merged with seed

-- temp drop trigger
drop trigger history_au_u;

delete from permission_status where code = 'grantedMetMinimum';

insert into permission_status (code, description) values ('granted', 'Granted');
insert into permission_status (code, description) values ('grantedLimited', 'Granted Limited');
insert into permission_status (code, description) values ('grantedLimitedPrint', 'Granted Limited');

-- updates so we can delete, won't be totally correct but will be fixed when recalc statuses
update asset_perm_ref set permission_status = 'granted' where permission_status in ('grantedExceededMinimum', 'grantedExceededMinimumLimitedPrint');
update au_source_perm_status set permission_status = 'granted' where permission_status in ('grantedExceededMinimum', 'grantedExceededMinimumLimitedPrint');
update asset_use set permission_status = 'granted' where permission_status in ('grantedExceededMinimum', 'grantedExceededMinimumLimitedPrint');

delete from permission_status where code in ('grantedExceededMinimum', 'grantedExceededMinimumLimitedPrint');

-- add trigger back
create trigger history_au_u after update on asset_use
for each row begin
  declare rowCount int;
  if new.is_canceled = 0 then
    set rowCount = (select count(*) from cw_history where cw_id = new.cw_id
	  and last_updated_user_id = new.last_updated_user_id
	  -- for some reason description = 'Asset(s) added/modified' does not match so use like
	  and description like 'Asset% added/modified'
	  and datediff(last_updated_date, current_timestamp) = 0);
    if (rowCount = 0) then
      insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
        values (new.cw_id, current_timestamp, current_timestamp, new.last_updated_user_id, new.created_user_id, 'Asset(s) added/modified');
    end if;
  else
      insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
        values (new.cw_id, current_timestamp, current_timestamp, new.last_updated_user_id, new.created_user_id, 'Asset cancelled');
  end if;
end;
