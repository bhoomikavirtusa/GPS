--update asset_use a, cs_assets b, asset c 
--set b.asset_id = a.asset_id
--where 
--a.asset_id = c.id and c.vendor_id = b.image_id and a.cw_id = b.cw_id
--and a.last_updated_user_id = 1;

delete from au_source_perm_status where asset_use_id in (
select id from asset_use where created_user_id = 1 and is_custom = 1 );

delete from asset_use_medium_exclusion where asset_use_id in (
select id from asset_use where created_user_id = 1 and is_custom = 1 );

delete from contract_2_asset where asset_base_id in (
select asset_id from asset_use where created_user_id = 1 and is_custom = 1 );

delete from asset_perm_ref where asset_id in (
select asset_id from asset_use where created_user_id = 1 and is_custom = 1 );

delete from asset_2_source where asset_id in (
select asset_id from asset_use where created_user_id = 1 and is_custom = 1 );

delete from asset_use where created_user_id = 1 and is_custom = 1;  

delete from contract_2_condition where contract_id not in (select contract_id from contract_2_asset);


delete from contract_file where contract_id not in (select contract_id from contract_2_asset);

delete from amendment where contract_id not in (select contract_id from contract_2_asset);


--update asset_use set latest_contract_id = null where cw_id in (select distinct cw_id from cs_assets);

delete from contract where id not in (select contract_id from contract_2_asset);

--delete from contract_2_asset where contract_id in (select id from contract where created_user_id = 1);

--delete from contract_2_condition where contract_id in (select id from contract where created_user_id = 1);
--delete from asset_perm_ref where contract_id in (select id from contract where created_user_id = 1);
--delete from contract where created_user_id = 1

--delete from asset_perm_ref where asset_use_id in (select id from asset_use where is_custom = 1 and created_user_id = 1);
--delete from au_source_perm_status where asset_use_id in (select id from asset_use where is_custom = 1 and created_user_id = 1);
--delete from asset_use where is_custom = 1 and created_user_id = 1;

--delete from cw_2_condition where condition_id not in (
--select condition_id from contract_2_condition)
--and condition_id not in (select condition_id from po_2_condition) 
--and condition_id not in (select condition_id from cw_2condition);;

delete from condition_value where id not in (
select condition_id from contract_2_condition) 
and id not in (select condition_id from po_2_condition) 
and id not in (select condition_id from cw_2condition);

delete from asset_2_source where asset_id in (select id from asset where id not in (select asset_id from asset_use));
delete from asset where id not in (select asset_id from asset_use);


-- delete from contract_2_asset where asset_base_id not in (select asset_id from asset_use);


-- delete from contract_2_condition where condition_id in (
--select id from condition_value where asset_base_id in (
--select id from asset_base where id not in (select asset_id from asset_use)));


--delete from condition_value where asset_base_id in (
--select id from asset_base where id not in (select asset_id from asset_use));

delete from asset_base where id not in (select asset_id from asset_use);










