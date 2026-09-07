-- merged with create

alter table au_source_perm_status modify permission_status nvarchar(50) not null;

alter table permission_status modify column code nvarchar(50) not null;
