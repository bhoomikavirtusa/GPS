-- merged with create

alter table asset_perm_ref drop foreign key fk_apr_2_po;

alter table asset_perm_ref
  add constraint fk_apr_2_po foreign key (po_id) references purchase_order (id) ON DELETE CASCADE;


alter table au_source_perm_status drop foreign key fk_ausps_2_l_po;
alter table au_source_perm_status drop foreign key fk_ausps_2_a_po;

alter table au_source_perm_status
  add constraint fk_ausps_2_l_po foreign key (latest_po_id) references purchase_order (id) ON DELETE CASCADE;

alter table au_source_perm_status
  add constraint fk_ausps_2_a_po foreign key (active_po_id) references purchase_order (id) ON DELETE CASCADE;
