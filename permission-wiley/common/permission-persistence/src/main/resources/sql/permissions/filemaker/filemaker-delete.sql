select distinct a.id from asset a join asset_use au on a.id = au.asset_id where a.import_source=0 and au.import_source=1;

update asset join asset_use on asset.id = asset_use.asset_id set asset.import_source=1 where asset_use.import_source=1 and asset.import_source=0;

drop table import_ignore;

create table import_ignore select asset_id from (select distinct asset_id, cw_id from asset_use where import_source=1)t  
group by asset_id having count(*) >=2;

insert into import_ignore select asset_base_id from contract_2_asset;

insert into import_ignore select asset_base_id from purchase_order_2_asset;

create index idx_asset_id ON import_ignore(asset_id);

delete from au_source_perm_status where asset_use_id in (
select id from asset_use where import_source=1 and asset_id not in (select asset_id from import_ignore));

delete from asset_use_medium_exclusion where asset_use_id in (
select id from asset_use where import_source=1 and asset_id not in (select asset_id from import_ignore));

delete from asset_perm_ref where asset_use_id in (
select id from asset_use where import_source=1 and asset_id not in (select asset_id from import_ignore));

delete from asset_use where import_source=1 and asset_id not in (select asset_id from import_ignore);

delete from asset_2_source where asset_id in (
	select id from asset where import_source = 1 and id not in (select asset_id from import_ignore));

delete from asset_perm_ref where asset_id in (
select id from asset where import_source = 1 and id not in (select asset_id from import_ignore));

delete from asset_file where asset_id in (
select id from asset where import_source = 1 and id not in (select asset_id from import_ignore));

delete from asset where import_source = 1 and id not in (select asset_id from import_ignore) and id not in (select distinct asset_id from asset_use);

delete from asset_base where id not in (select id from asset);

select count(*) from asset; - 34583
select count(*) from asset_use; - 40822



## update pwlist set status=null, error=null where status != 'ignore'
## select * from pwlist where status='error'


## create table import_ignore select asset_id from (select distinct asset_id, cw_id from asset_use where import_source=1)t  
group by asset_id having count(*) >=2

## select * from asset_use join import_ignore on tmp.asset_id = asset_use.asset_id order by asset_use.asset_id