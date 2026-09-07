create table tmp select asset_id from (select distinct asset_id, cw_id from asset_use where import_source=1)t  
group by asset_id having count(*) >=2;

delete from au_source_perm_status where asset_use_id in (
select id from asset_use where import_source=1 and asset_id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp));

delete from asset_use_medium_exclusion where asset_use_id in (
select id from asset_use where import_source=1 and asset_id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp));

delete from asset_perm_ref where asset_use_id in (
select id from asset_use where import_source=1 and asset_id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp));

delete from asset_use where import_source=1 and asset_id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp);

delete from asset_2_source where asset_id in (
	select id from asset where import_source = 1 and id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp));

delete from asset_perm_ref where asset_id in (
select id from asset where import_source = 1 and id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp));

delete from asset_file where asset_id in (
select id from asset where import_source = 1 and id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp));

delete from asset where import_source = 1 and id not in (select asset_base_id from contract_2_asset union select asset_base_id from purchase_order_2_asset union select asset_id from tmp);

delete from asset_base where id not in (select id from asset);


## update pwlist set status=null, error=null where status != 'ignore'
## select * from pwlist where status='error'


## create table tmp select asset_id from (select distinct asset_id, cw_id from asset_use where import_source=1)t  
group by asset_id having count(*) >=2

## select * from asset_use join tmp on tmp.asset_id = asset_use.asset_id order by asset_use.asset_id