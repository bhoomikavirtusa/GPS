-- merged with create and seed
alter table asset_use change p_num sort_order nvarchar(10);

drop trigger history_au_u;

update asset_use set sort_order = null where length(trim(sort_order)) = 0;

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

update user_profile set name = 'sortOrder' where name = 'pnum';

update user_profile set width = 7 where name = 'sortOrder';
