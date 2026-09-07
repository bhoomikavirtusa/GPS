-- merged with create

alter table asset_use modify column status_explanation nvarchar(1000);
alter table au_source_perm_status modify column status_explanation nvarchar(1000);
alter table asset_perm_ref modify column status_explanation nvarchar(1000);
