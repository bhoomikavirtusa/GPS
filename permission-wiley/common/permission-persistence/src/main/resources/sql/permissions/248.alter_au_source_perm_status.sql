-- merged with create

alter table au_source_perm_status drop foreign key fk_ausps_2_au;

alter table au_source_perm_status
  add constraint fk_ausps_2_au foreign key (asset_use_id) references asset_use (id) ON DELETE CASCADE;

alter table asset_use drop foreign key fk_asset_use_2_previous;

alter table asset_use
  add constraint fk_asset_use_2_previous foreign key (previous_wileypub_au_id) references asset_use (id) ON DELETE SET NULL;

alter table asset_perm_ref drop foreign key fk_apr_2_c;

alter table asset_perm_ref
  add constraint fk_apr_2_c foreign key (contract_id) references contract (id) ON DELETE CASCADE;

alter table au_source_perm_status drop foreign key fk_ausps_2_l_contract;

alter table au_source_perm_status
  add constraint fk_ausps_2_l_contract foreign key (latest_contract_id) references contract (id) ON DELETE CASCADE;

alter table au_source_perm_status drop foreign key fk_ausps_2_a_contract;

alter table au_source_perm_status
  add constraint fk_ausps_2_a_contract foreign key (active_contract_id) references contract (id) ON DELETE CASCADE;
