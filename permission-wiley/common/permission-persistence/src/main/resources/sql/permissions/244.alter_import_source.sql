-- merged with seed and create

update import_sources set description = 'GE Filemaker' where code = 1;

alter table asset drop foreign key fk_asset_2_import_source;
alter table asset_use drop foreign key fk_ause_2_import_source;
alter table contract drop foreign key fk_contract_2_import_source;

rename table import_sources to import_source;

alter table asset
  add constraint fk_asset_2_import_source foreign key (import_source) references import_source (code);

alter table contract
  add constraint fk_contract_2_import_source foreign key (import_source) references import_source (code);

alter table asset_use
  add constraint fk_ause_2_import_source foreign key (import_source) references import_source (code);
