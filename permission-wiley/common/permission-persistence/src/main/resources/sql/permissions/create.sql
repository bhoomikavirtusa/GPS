-- MySQL version 5.6.13 or above is required.
-- All varchars will be one of the following lengths:
-- 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, etc
-- - Except columns that need to match CMS or other systems.
-- All datetime columns are datetime(3) so as to include milliseconds.
-- MySQL automatically creates indexes on all foreign key columns, so we do not
-- bother to create explicit indexes on these columns.
-- Need to replace double pipeline(||) with "concat" keyword to join 2 strings ( updated : 29 AUG 2019 )  
create table gbpm_category (
  code int not null, 
  constraint pk_data_type primary key (code),
  description nvarchar(100)
);

create table data_type (
  code nvarchar(20) not null,
  constraint pk_data_type primary key (code),
  description nvarchar(100)
);

create table account (
  id int not null auto_increment,
  constraint pk_account primary key (id),
  acct_number nvarchar(20),
  sub_code nvarchar(20),
  description nvarchar(100)
);

alter table account
  add constraint un_account unique (acct_number, sub_code);

create table user_type (
  code nvarchar(20) not null,
  constraint pk_user_type primary key (code),
  description nvarchar(100)
);

create table user_group (
  id int not null auto_increment,
  constraint pk_user_group primary key (id),
  name nvarchar(20) not null,
  description nvarchar(200)
);

alter table user_group
  add constraint un_user_group_name unique (name);

create table favorite_group (
  id int not null auto_increment,
  constraint pk_source_group primary key (id),
  description nvarchar(100) not null
);

alter table favorite_group
  add constraint un_favorite_group unique (description);

create table user_table (
  id int not null auto_increment,
  constraint pk_user primary key (id),
  -- Would like to have default current_timestamp but MySQL doesn't support this
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  user_type nvarchar(20) not null,
  enabled bool not null default true,
  user_group_id int,
  first_name nvarchar(50) not null,
  last_name nvarchar(50) not null,
  -- see triggers on email column below
  email nvarchar(200),
  -- code for authors/freelancers will be the same as email so should be same size
  code nvarchar(200) not null,
  phone nvarchar(15),
  street nvarchar(100),
  city nvarchar(100),
  state nvarchar(50),
  zip nvarchar(10),
  country_code nvarchar(5),
  favorite_group_id int
);

alter table user_table
  add constraint un_user_code unique (code);

alter table user_table
  add constraint fk_user_2_utype foreign key (user_type) references user_type (code);

alter table user_table
  add constraint fk_user_2_group foreign key (user_group_id) references user_group (id);
  
alter table user_table
  add constraint fk_user_2_fav_group foreign key (favorite_group_id) references favorite_group (id);

-- Can't put unique constraint unless there is only one null
-- alter table user add constraint un_user_email unique (email);
-- Use non-unique index + triggers instead
create index idx_user_email on user_table (email);

delimiter $$
create trigger user_i_email_unique before insert on user_table
for each row begin
  -- (declare inside if does not work)
  declare msg varchar(500);
  if (new.email is not null and exists(select id from user_table where email = new.email)) then
    -- for some reason setting message_text directly does not work with concat
    set msg = concat('Duplicate email [', new.email, '] not allowed in user_table');
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = msg;
  end if;
end;$$
delimiter ;

delimiter $$
create trigger user_u_email_unique before update on user_table
for each row begin
  -- (declare inside if does not work)
  declare msg varchar(500);
  if (new.email is not null and new.email != old.email and exists(select id from user_table where email = new.email)) then
    -- for some reason setting message_text directly does not work with concat
    set msg = concat('Duplicate email [', new.email, '] not allowed in user_table');
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = msg;
  end if;
end;$$
delimiter ;

create table data_source (
  code nvarchar(20) not null,
  constraint pk_data_source primary key (code),
  description nvarchar(100)
);

create table publication_status (
  code nvarchar(20) not null,
  constraint pk_pub_status primary key (code),
  description nvarchar(100)
);

-- 'usage' is a reserved word in MySQL hence 'usage_type'
create table usage_type (
  code nvarchar(20) not null,
  constraint pk_usage primary key (code),
  description nvarchar(100),
  abbreviation nvarchar(10) not null,
  sort_order int not null
);

create table business_unit (
  code nvarchar(20) not null,
  constraint pk_biz_unit primary key (code),
  name nvarchar(100),
  email nvarchar(100),
  email_covers nvarchar(200) default 'none',
  warn_roles nvarchar(200) default 'none',
  warn_roles_covers nvarchar(200) default 'none',
  warn_last_update_user bool not null default true,
  warn_last_update_user_covers bool not null default true
);

create table medium (
  code nvarchar(20) not null,
  constraint pk_medium primary key (code),
  name nvarchar(100)
);

create table submedium (
  id int not null auto_increment,
  constraint pk_submedium primary key (id),
  code nvarchar(20) not null,
  data_source nvarchar(20) not null,
  name nvarchar(100)
);

alter table submedium
  add constraint un_submedium unique (code, data_source);

alter table submedium
  add constraint fk_submed_2_data_source foreign key (data_source) references data_source (code);

create table product_family (
  code nvarchar(20) not null,
  constraint pk_prod_fam primary key (code),
  name nvarchar(100)
);

create table product_type (
  code nvarchar(20) not null,
  constraint pk_prod_type primary key (code),
  name nvarchar(100)
);

create table product_line (
  id int not null auto_increment,
  constraint pk_product_line primary key (id),
  code nvarchar(20) not null,
  data_source nvarchar(20) not null,
  name nvarchar(100),
  business_unit nvarchar(20)
);

alter table product_line
  add constraint fk_prod_line_2_data_source foreign key (data_source) references data_source (code);

alter table product_line
  add constraint fk_prod_2_biz_unit foreign key (business_unit) references business_unit (code);

alter table product_line
  add constraint un_product_line unique (code, data_source);

create table geo_location (
  code nvarchar(20) not null,
  constraint pk_geo_loc primary key (code),
  name nvarchar(100)
);

create table relation_code (
  code nvarchar(20) not null,
  constraint pk_relation_code primary key (code),
  name nvarchar(100)
);

create table subject_code (
  id int not null auto_increment,
  constraint pk_subject_code primary key (id),
  code nvarchar(20) not null,
  data_source nvarchar(20) not null,
  short_name nvarchar(100),
  name nvarchar(200)
);

alter table subject_code
  add constraint un_subject_code unique (code, data_source);
  
alter table subject_code
  add constraint fk_subcode_2_data_source foreign key (data_source) references data_source (code);

create table editor (
  id int not null auto_increment,
  constraint pk_subject_code primary key (id),
  code nvarchar(20) not null,
  data_source nvarchar(20) not null,
  name nvarchar(200)
);

alter table editor
  add constraint un_editor unique (code, data_source);
  
alter table editor
  add constraint fk_editor_2_data_source foreign key (data_source) references data_source (code);

create table product_edition (
  id int not null auto_increment,
  constraint pk_product_edition primary key (id),
  external_id nvarchar(50) not null,
  name nvarchar(100) not null,
  edition_number int not null,
  product_family nvarchar(20),
  product_line_id int
);

alter table product_edition
  add constraint un_p_ed_ext_id unique (external_id);

alter table product_edition
  add constraint fk_p_ed_2_p_fam foreign key (product_family) references product_family (code);

alter table product_edition
  add constraint fk_p_ed_2_pline foreign key (product_line_id) references product_line (id);

create table permission_payer (
  code nvarchar(20) not null,
  constraint pk_perm_payer primary key (code),
  description nvarchar(100)
);

create table common_work_status (
  code nvarchar(20) not null,
  constraint pk_common_work_status primary key (code),
  description nvarchar(100)
);

create table common_work (
  id int not null auto_increment,
  constraint pk_common_work primary key (id),
  -- 64 is only need for our self-generated codes (rare cases)
  -- most of the time 20 is plenty
  code nvarchar(64) not null,
  name nvarchar(300),
  interior_cw_status nvarchar(20) not null default 'in_progress',
  cover_cw_status nvarchar(20) not null default 'in_progress',
  notes nvarchar(500) default null,
  conditions_init bool not null default false,
  -- min_grant_years is a "mininum permission requirement", 0 means no mininum
  min_grant_years int not null default 0
);

alter table common_work add constraint un_cw_code unique (code);

alter table common_work
  add constraint fk_cw_2_cwstatus_int foreign key (interior_cw_status) references common_work_status (code) on update cascade;
alter table common_work
  add constraint fk_cw_2_cwstatus_cov foreign key (cover_cw_status) references common_work_status (code) on update cascade;

create table product (
  id int not null auto_increment,
  constraint pk_product primary key (id),
  external_id nvarchar(50) not null,
  cw_id int not null,
  is_cw_primary bool not null default false,
  -- MySQL doesn't support default current_timestamp
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  isbn10 nvarchar(10),
  isbn13 nvarchar(13),
  pnumber nvarchar(20),
  data_source nvarchar(20) not null,
  sku nvarchar(20),
  pub_status nvarchar(20) not null,
  process_code nvarchar(10),
  title nvarchar(300) not null,
  short_title nvarchar(200),
  volume nvarchar(20),
  copyright_year int,
  production_date datetime(3),
  print_date datetime(3),
  subject_code_id int,
  medium nvarchar(20),
  submedium_id int,
  business_unit nvarchar(20) not null,
  edition int,
  edition_number int,
  previous_edition_wid nvarchar(50),
  next_edition_wid nvarchar(50),
  product_line_id int,
  product_family nvarchar(20),
  product_type nvarchar(20),
  geo_location nvarchar(20),
  permission_payer nvarchar(20),
  language_spoken nvarchar(5),
  discount_group_code nvarchar(10),
  discount_sub_group_code nvarchar(10),
  short_author_name nvarchar(256),
  consolidated_release_date datetime(3) default null,
  transmittal_date datetime(3) default null,
  editor nvarchar(10),
  photo_illus_total_count int,
  audit_code nvarchar(10),
  ebook_sales int,
  component_flag nvarchar(10)
);

alter table product
  add constraint un_product_ext_id unique (external_id);

-- Can't put unique constraint unless there is only one null
create index idx_product_isbn10 on product(isbn10);
create index idx_product_isbn13 on product(isbn13);

alter table product
  add constraint fk_prod_2_data_source foreign key (data_source) references data_source (code);

alter table product
  add constraint fk_product_2_pubs foreign key (pub_status) references publication_status (code);

alter table product
  add constraint fk_product_2_scode foreign key (subject_code_id) references subject_code (id);

alter table product
  add constraint fk_prod_2_med foreign key (medium) references medium (code);

alter table product
  add constraint fk_prod_2_submed foreign key (submedium_id) references submedium (id);

alter table product
  add constraint fk_product_2_bu foreign key (business_unit) references business_unit (code);

alter table product
  add constraint fk_product_2_ed foreign key (edition) references product_edition (id);

alter table product
  add constraint fk_product_2_pl foreign key (product_line_id) references product_line (id);

alter table product
  add constraint fk_product_2_pf foreign key (product_family) references product_family (code);

alter table product
  add constraint fk_product_2_pt foreign key (product_type) references product_type (code);

alter table product
  add constraint fk_product_2_gl foreign key (geo_location) references geo_location (code);

alter table product
  add constraint fk_product_2_pp foreign key (permission_payer) references permission_payer (code);
  
alter table product
  add constraint fk_product_2_cw foreign key (cw_id) references common_work (id);

create table bundle (
  code nvarchar(20) not null,
  constraint pk_bundle primary key (code),
  name nvarchar(100)
);

create table product_2_bundle (
  product_id int not null,
  bundle_code nvarchar(20) not null
);

alter table product_2_bundle
  add constraint pk_product_2_bundle primary key (product_id, bundle_code);

alter table product_2_bundle
  add constraint fk_prod2bun_product foreign key (product_id) references product (id);

alter table product_2_bundle
  add constraint fk_prod2bun_bundle foreign key (bundle_code) references bundle (code);
  
create table relation (
  id int not null auto_increment,
  constraint pk_relation primary key (id),
  product_id int not null,
  code nvarchar(20) not null,
  related_wid nvarchar(50) not null
);

alter table relation
  add constraint fk_relation_2_product foreign key (product_id) references product (id);

alter table relation
  add constraint fk_relation_2_rel_code foreign key (code) references relation_code (code);

-- not creating foreign key for related_wid because may not have all referenced products in our db

alter table relation
  add constraint un_relation unique (product_id, code, related_wid);

create index idx_relation_p_id on relation (product_id);
  
create table cw_file (
  id int not null auto_increment,
  constraint pk_cw_file primary key (id),
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  cw_id int not null,
  file_name nvarchar(200) not null,
  mime_type nvarchar(100) not null,
  description nvarchar(500),
  data blob(10111000) not null
);

alter table cw_file
  add constraint fk_cw_file_2_cw foreign key (cw_id) references common_work (id);

create table photo_estimate_type (
  code nvarchar(20) not null,
  constraint pk_photo_estimate primary key (code),
  description nvarchar(50) not null,
  data_type nvarchar(20) not null,
  sort_order int not null
);

alter table photo_estimate_type
  add constraint fk_pet_2_data_type foreign key (data_type) references data_type (code);

create table cw_photo_estimate (
  id int not null auto_increment,
  constraint pk_cw_photo_est primary key (id),
  cw_id int not null,
  photo_estimate_type nvarchar(20) not null,
  estimated_value decimal(8,2) not null default 0,
  date datetime(3) not null,
  reproduction_fees decimal(8,2) not null default 0,
  research_fees decimal(8,2) not null default 0,
  freelance_research_fees  decimal(8,2) not null default 0,
  photos int not null default 0
);

alter table cw_photo_estimate
  add constraint fk_cw_pe_2_cw foreign key (cw_id) references common_work (id);

alter table cw_photo_estimate
  add constraint fk_cw_pe_2_pet foreign key (photo_estimate_type) references photo_estimate_type (code);

create table cw_summary (
  cw_id int not null,
  constraint pk_cw_summary primary key (cw_id),
  research_status int,
  research_progress_notes nvarchar(250) default null,
  credit_list_submitted bool not null default false,
  photo_research_due datetime(3) default null,
  permissions_due datetime(3) default null,
  page_proof_schedule_date datetime(3) default null,
  release_to_printer_date datetime(3) default null,
  author_permissions_due datetime(3) default null,
  manuscript_submission_deadline datetime(3) default null
);

alter table cw_summary
  add constraint fk_cw_sum_2_cw foreign key (cw_id) references common_work (id);

create table component_category (
  code nvarchar(20) not null,
  constraint pk_component_cat primary key (code),
  description nvarchar(50) not null
);

create table component (
  id int not null auto_increment,
  constraint pk_component primary key (id),
  external_id nvarchar(64) not null,
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  cw_id int not null,
  name nvarchar(255) not null,
  category nvarchar(20) not null,
  sort_order int not null
);

alter table component
  add constraint un_component_ext unique (external_id);

alter table component
  add constraint un_component_name unique (cw_id, name);

alter table component
  add constraint fk_component_2_cw foreign key (cw_id) references common_work (id);

alter table component
  add constraint fk_comp_2_cat foreign key (category) references component_category (code);
  
create index idx_comp_cw_id on component (cw_id);

create table currency (
  code nvarchar(20) not null,
  constraint pk_currency primary key (code),
  description nvarchar(100),
  sort_order int not null
);

create table wiley_entity (
  code nvarchar(20) not null,
  constraint pk_wiley_entity primary key (code),
  description nvarchar(100)
);

create table user_location (
  code nvarchar(20) not null,
  constraint pk_user_loc primary key (code),
  name nvarchar(100)
);

create table size_enum (
  code nvarchar(20) not null,
  constraint pk_size_enum primary key (code),
  description nvarchar(100),
  sort_order int not null
);

create table user_defaults (
  id int not null auto_increment,
  constraint pk_user_defaults primary key (id),
  user_id int not null,
  return_address nvarchar(200),
  user_signature nvarchar(200),
  cost_center nvarchar(20),
  account_id int,
  component_category nvarchar(20),
  show_request bool not null default true,
  custom_mode bool not null default false,
  custom_filter nvarchar(20),
  currency_code nvarchar (20) not null default 'USD',
  country_code nvarchar (5) not null default 'US',
  wiley_entity_code nvarchar(20) not null default 'WILEY_US',
  show_estimated_cost bool not null default true,
  size nvarchar(20) not null default 'N/A',
  user_location_code nvarchar(20) not null default 'US',
  business_unit_code nvarchar(20),
  date_format nvarchar(20) not null default 'mm/dd/yyyy'
);

alter table user_defaults
  add constraint fk_user_def_2_curr foreign key (currency_code) references currency (code);

alter table user_defaults
  add constraint fk_user_def_2_user foreign key (user_id) references user_table (id);

alter table user_defaults
  add constraint fk_user_def_2_acct foreign key (account_id) references account (id);

alter table user_defaults
  add constraint fk_user_def_2_ccat foreign key (component_category) references component_category (code);

alter table user_defaults
  add constraint fk_user_2_entity foreign key (wiley_entity_code) references wiley_entity (code);

alter table user_defaults
  add constraint fk_user_def_2_size foreign key (size) references size_enum (code);
  
alter table user_defaults
  add constraint fk_user_def_2_user_loc foreign key (user_location_code) references user_location (code);

alter table user_defaults
  add constraint fk_user_def_2_bus_unit foreign key (business_unit_code) references business_unit (code);

create table role_type (
  code nvarchar(20) not null,
  constraint pk_role_type primary key (code),
  description nvarchar(100)
);

create table role (
  id int not null auto_increment,
  constraint pk_role primary key (id),
  code nvarchar(20) not null,
  role_type nvarchar(20) not null,
  is_global bool not null,
  description nvarchar(100)
);

alter table role
  add constraint fk_role_2_rtype foreign key (role_type) references role_type (code);

alter table role
  add constraint un_role unique (code, role_type);

create table user_2_role (
  id int not null auto_increment,
  constraint pk_user_2_role primary key (id),
  user_id int not null,
  role_id int not null,
  product_id int
);

alter table user_2_role
  add constraint fk_ur_2_user foreign key (user_id) references user_table (id);

alter table user_2_role
  add constraint fk_ur_2_role foreign key (role_id) references role (id);

alter table user_2_role
  add constraint fk_ur_2_product foreign key (product_id) references product (id);

alter table user_2_role
  add constraint un_user_2_role unique (user_id, role_id, product_id);

create table privilege (
  code nvarchar(20) not null,
  constraint pk_privilege primary key (code),
  description nvarchar(100),
  is_global bool not null default false,
  author_ok bool not null default false,
  sort_order int not null default 0
);

create table watched_cw (
  user_id int not null,
  cw_id int not null
);

alter table watched_cw
  add constraint pk_watched_cw primary key (user_id, cw_id);

alter table watched_cw
  add constraint fk_watched_2_user foreign key (user_id) references user_table (id);

alter table watched_cw
  add constraint fk_watched_2_cw foreign key (cw_id) references common_work (id);

create index idx_watched_u_id on watched_cw (user_id);

create table delivery_method (
  code nvarchar(20) not null,
  constraint pk_delivery_method primary key (code),
  description nvarchar(100),
  sort_order int not null
);

create table source_group (
  id int not null auto_increment,
  constraint pk_source_group primary key (id),
  name nvarchar(50) not null,
  nofly bool not null default false,
  comment nvarchar(1000)
);

alter table source_group
  add constraint un_sgroup_name unique (name);

create table source (
  id int not null auto_increment,
  constraint pk_source primary key (id),
  external_id nvarchar(64) not null,
  name nvarchar(200) not null,
  display_name nvarchar(200) not null,
  credit_line nvarchar(1000),
  delivery_method nvarchar(20),
  permission_request_url nvarchar(300) default null,
  source_group int default null,
  nofly bool not null default false,
  comment nvarchar(1000),
  nofly_photographer_last_name nvarchar(50),
  sued_wiley bool not null default false,
  permission_type nvarchar(30),
  website nvarchar(200),
  phone nvarchar(50),
  fax nvarchar(50),
  country_code nvarchar(5),
  jde_vendor_number nvarchar(8)
);

alter table source
  add constraint un_source_ext_id unique (external_id);

-- No using unique constraint currently but will as soon as we finish de-dup on name
-- alter table source
--   add constraint un_source_name unique (name);

-- We can remove this index when we are actually using the unique constraint on name (above)
-- but we aren't using this constraint currently
alter table source add index (name(100) );

alter table source
  add constraint fk_source_2_dm foreign key (delivery_method) references delivery_method (code);
  
alter table source
  add constraint fk_source_2_sgroup foreign key (source_group) references source_group (id);

alter table source add FULLTEXT(nofly_photographer_last_name);

create table source_file (
  id int not null auto_increment,
  constraint pk_source_file primary key (id),
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  source_id int not null,
  file_name nvarchar(255) not null,
  description nvarchar(1000) not null,
  mime_type nvarchar(100) not null,
  file_data blob(10111000) not null
);

alter table source_file
  add constraint fk_srcfile_2_src foreign key (source_id) references source (id);

create table country (
  code nvarchar(5) not null,
  constraint pk_country primary key (code),
  description nvarchar(100)
);

create table address_type (
  code nvarchar(5) not null,
  constraint pk_address_type primary key (code),
  description nvarchar(100)
);

create table address (
  id int not null auto_increment,
  constraint pk_address primary key (id),
  address_type_code nvarchar(5) not null default 'M',
  line_one nvarchar(200) not null,
  line_two nvarchar(200),
  line_three nvarchar(200),
  city nvarchar(100),
  state_province nvarchar(50),
  postal_code nvarchar(10),
  country_code nvarchar(5)
);

alter table address
  add constraint fk_address_2_address_2_type foreign key (address_type_code) references address_type (code);

alter table address
  add constraint fk_address_2_country foreign key (country_code) references country (code);

create table contact (
  id int not null auto_increment,
  constraint pk_contact primary key (id),
  source_id int not null,
  first_name nvarchar(100) not null,
  middle_name nvarchar(100),
  last_name nvarchar(100),
  job_title nvarchar(100),
  gender nvarchar(1),
  email nvarchar(100),
  phone nvarchar(50),
  fax nvarchar(20),
  mobile_phone nvarchar(50),
  home_phone nvarchar(50),
  note nvarchar(200),
  address_id int
);

alter table contact
  add constraint fk_contact_2_address foreign key (address_id) references address (id);

alter table contact
  add constraint fk_contact_2_source foreign key (source_id) references source (id);

create table source_2_address (
	source_id int not null,
	address_id int not null
);

alter table source_2_address
  add primary key pk_source_2_address (source_id, address_id);

alter table source_2_address
  add constraint fk_source_2_address_2_source foreign key (source_id) references source (id);
  
alter table source_2_address
  add constraint fk_source_2_address_2_address foreign key (address_id) references address (id);

create table media_type (
  code nvarchar(20) not null,
  constraint pk_media_type primary key (code),
  description nvarchar(100),
  sort_order int not null
);

create table owner_type (
  code nvarchar(20) not null,
  constraint pk_owner_type primary key (code),
  description nvarchar(100),
  sort_order int not null
);

create table rendition_type (
  code nvarchar(50) not null,
  constraint pk_rendition_type primary key (code),
  description nvarchar(100),
  sort_order int not null
);

create table asset_base (
  id int not null auto_increment,
  constraint pk_asset_base primary key (id),
  external_id nvarchar(64) not null,
  asset_base_type nvarchar(20) not null default 'ASSET',
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  last_updated_user_id int not null,
  created_user_id int not null
);

alter table asset_base
  add constraint un_assetb_ext_id unique (external_id);

alter table asset_base
  add constraint fk_assetb_2_user_u foreign key (last_updated_user_id) references user_table (id);
  
alter table asset_base
  add constraint fk_assetb_2_user_c foreign key (created_user_id) references user_table (id);

create table asset_group (
  id int not null,
  constraint pk_asset_group primary key (id),
  name nvarchar(100) not null,
  description nvarchar(200) not null,
  source_id int
);

alter table asset_group
  add constraint fk_assetg_2_ab foreign key (id) references asset_base (id);

create table model_release (
  code nvarchar(20) not null,
  constraint pk_model_release primary key (code),
  name nvarchar(100)
);

create table copyright_type (
  code  nvarchar(20) not null,
  constraint pk_data_type primary key (code),
  description nvarchar(100)
);

create table import_source (
  code int not null,
  constraint pk_data_type primary key (code),
  description nvarchar(100)
);

create table asset (
  id int not null,
  constraint pk_asset primary key (id),
  media_type nvarchar(20),
  asset_group_id int,
  copied_from_id int,
  description nvarchar(1000) not null,
  keywords nvarchar(256) default null,
  owner_type nvarchar(20),
  vendor_id nvarchar(150),
  artist nvarchar(400),
  credit_line nvarchar(1000),
  must_display_credit bool not null default false,
  is_managed bool not null default false,
  is_fee_required bool not null default false,
  is_royalty_free bool not null default false,
  will_be_royalty_free bool not null default false,
  is_restricted_use bool not null default false,
  restricted_use_details nvarchar(2000),
  is_active bool not null default true,
  model_release nvarchar(20) not null default 'not_needed',
  property_release bool not null default false,
  work_for_hire bool not null default false,
  will_be_work_for_hire bool not null default false,
  obtained_by_author bool not null default false,
  original_publication_title nvarchar(300),
  original_publication_author nvarchar(500),
  original_article_title nvarchar(300) default null,
  original_article_author nvarchar(300) default null,
  original_publication_isbn nvarchar(13),
  original_figure_number nvarchar(50),
  original_page_number nvarchar(25),
  from_1 nvarchar(25),
  to_1 nvarchar(25),
  from_2 nvarchar(25),
  to_2 nvarchar(25),
  from_3 nvarchar(25),
  to_3 nvarchar(25),
  archive bool not null default false,
  citation nvarchar(1000) default null,
  biblio nvarchar(1000) default null,
  total_seats int not null default 0,
  total_print_run int not null default 0,
  total_used_seats int not null default 0,
  filename nvarchar(100),
  copyright_type nvarchar(20) not null default '0',
  import_source int null,
  reviewed_unknown bool not null default false,
  author_provided_unknown bool not null default false,
  is_stm_guidelines bool not null default false
);

alter table asset
  add constraint fk_asset_2_ab foreign key (id) references asset_base (id);

alter table asset
  add constraint fk_asset_2_ag foreign key (asset_group_id) references asset_group (id);

alter table asset
  add constraint fk_asset_2_asset foreign key (copied_from_id) references asset (id);

alter table asset
  add constraint fk_asset_2_ot foreign key (owner_type) references owner_type (code);

alter table asset
  add constraint fk_asset_2_media_type foreign key (media_type) references media_type (code);

alter table asset
  add constraint fk_asset_mrelease foreign key (model_release) references model_release (code);
  
alter table asset
  add constraint fk_asset_2_copyright_type foreign key (copyright_type) references copyright_type (code);

alter table asset
  add constraint fk_asset_2_import_source foreign key (import_source) references import_source (code);

create table asset_2_source (
  asset_id int not null,
  source_id int not null,
  notes nvarchar(100)
);

alter table asset_2_source
  add constraint pk_asset_2_source primary key (asset_id, source_id);

alter table asset_2_source
  add constraint fk_a2s_2_asset foreign key (asset_id) references asset (id);

alter table asset_2_source
  add constraint fk_a2s_2_source foreign key (source_id) references source (id);

create index idx_a2s_asset_id on asset_2_source (asset_id);
create index idx_a2s_source_id on asset_2_source (source_id);

create table asset_file (
  id int not null auto_increment,
  constraint pk_asset_file primary key (id),
  asset_id int not null,
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  object_name nvarchar(200) not null,
  file_format nvarchar(100) not null,
  data blob(500111000) not null,
  rendition_type nvarchar(50) not null
);

alter table asset_file
  add constraint fk_afile_2_a foreign key (asset_id) references asset (id);

alter table asset_file
  add constraint fk_afile_2_rtype foreign key (rendition_type) references rendition_type (code);

create table master_agreement_deal (
  id int not null auto_increment,
  constraint pk_madeal_id primary key (id),
  source_group_id int not null,
  name nvarchar(100) not null,
  pricing_info nvarchar(2000),
  grant_info nvarchar(2000),
  grant_duration_years int not null default 0,
  start_date datetime(3) not null,
  end_date datetime(3),
  file_name nvarchar(200) not null,
  mime_type nvarchar(100) not null,
  file_data blob(10111000) not null,
  notes nvarchar(1000)
);

alter table master_agreement_deal
  add constraint fk_madeal_2_sgroup foreign key (source_group_id) references source_group (id);

create table madeal_2_ulocation (
	madeal_id int not null,
	ulocation_code nvarchar(20) not null
);

alter table madeal_2_ulocation
  add constraint pk_madeal_2_ulocation primary key (madeal_id, ulocation_code);

alter table madeal_2_ulocation
  add constraint fk_madeal2ul_2_madeal foreign key (madeal_id) references master_agreement_deal (id);

alter table madeal_2_ulocation
  add constraint fk_madeal2ul_2_ulocation foreign key (ulocation_code) references user_location (code);

create table madeal_2_businessunit (
	madeal_id int not null,
	businessunit_code nvarchar(20) not null
);

alter table madeal_2_businessunit
  add constraint pk_madeal_2_bunit primary key (madeal_id, businessunit_code);

alter table madeal_2_businessunit
  add constraint fk_madeal2bu_2_madeal foreign key (madeal_id) references master_agreement_deal (id);

alter table madeal_2_businessunit
  add constraint fk_madeal2ul_2_bu foreign key (businessunit_code) references business_unit (code);

create table royalty_free_deal (
  id int not null auto_increment,
  constraint pk_source_group_id primary key (id),
  source_group_id int not null,
  seats           int not null default 0,
  total_print_run int not null default 0,
  description nvarchar(100),
  disabled_flag bool not null default false
);

alter table royalty_free_deal
  add constraint fk_source_group foreign key (source_group_id) references source_group (id);

create table enum_language (
  code nvarchar(10) not null,
  constraint pk_enum_lang primary key (code),
  description nvarchar(100)
);

create table purchase_order (
  id int not null auto_increment,
  constraint pk_purchase_order primary key (id),
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  last_updated_user_id int not null,
  created_user_id int not null,
  cw_id int not null,
  date datetime(3) not null,
  source_id int not null,
  is_permission_request bool not null default false,
  note nvarchar(500),
  return_address nvarchar(200) default '',
  user_signature nvarchar(200) default '',
  is_outside_record bool not null default false,
  estimated_price decimal(8,2) not null default 0,
  estimated_currency nvarchar(20),
  show_estimated_cost bool not null default false,
  source_contact_info nvarchar(100) DEFAULT '',
  source_address_info nvarchar(200) DEFAULT '',
  show_isbn bool not null default true,
  show_copyright_year bool not null default true,
  language_code nvarchar(10)
);

alter table purchase_order
  add constraint fk_po_2_user_u foreign key (last_updated_user_id) references user_table (id);

alter table purchase_order
  add constraint fk_po_2_user_c foreign key (created_user_id) references user_table (id);

alter table purchase_order
  add constraint fk_po_2_source foreign key (source_id) references source (id);

alter table purchase_order
  add constraint fk_po_2_cw foreign key (cw_id) references common_work (id);

alter table purchase_order
  add constraint fk_po_2_curr foreign key (estimated_currency) references currency (code);

alter table purchase_order
  add constraint fk_po_2_lang foreign key (language_code) references enum_language (code);

create table purchase_order_2_asset (
  purchase_order_id int not null,
  asset_base_id int not null,
  copy_attached bool not null default false
);

alter table purchase_order_2_asset
  add constraint pk_po_2_asset primary key (purchase_order_id, asset_base_id);

alter table purchase_order_2_asset
  add constraint fk_po2a_2_po foreign key (purchase_order_id) references purchase_order (id);
  
alter table purchase_order_2_asset
  add constraint fk_po2a_2_assetb foreign key (asset_base_id) references asset_base (id);

create index idx_po2a_po_id on purchase_order_2_asset (purchase_order_id);
create index idx_po2a_ab_id on purchase_order_2_asset (asset_base_id);

create table contract_type (
  code nvarchar(20) not null,
  constraint pk_contract_type primary key (code),
  description nvarchar(100)
);

create table contract (
  id int not null auto_increment,
  constraint pk_contract primary key (id),
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  last_updated_user_id int not null,
  created_user_id int not null,
  description nvarchar(200),
  start_date datetime(3) not null,
  end_date datetime(3),
  cw_id int not null,
  contract_type nvarchar(20) not null default 'frontlist',
  -- Invoice fields
  is_permission_form bool not null default false,
  number nvarchar(50),
  source_id int not null,
  date datetime(3) not null,
  price decimal(8,2) not null,
  currency nvarchar(20) not null,
  purchase_order_id int,
  payment_type nvarchar(10),
  payment_date datetime(3),
  ma_deal_id int,
  import_source int,
  license_xml nvarchar(5000),
  copyright_type nvarchar(20) not null default '0'
);

alter table contract
  add constraint fk_contract_2_user_u foreign key (last_updated_user_id) references user_table (id);
  
alter table contract
  add constraint fk_contract_2_user_c foreign key (created_user_id) references user_table (id);

alter table contract
  add constraint fk_contract_2_cw foreign key (cw_id) references common_work (id);

alter table contract
  add constraint fk_contract_2_src foreign key (source_id) references source (id);

alter table contract
  add constraint fk_contract_2_curr foreign key (currency) references currency (code);

alter table contract
  add constraint fk_contract_2_po foreign key (purchase_order_id) references purchase_order (id);

alter table contract
  add constraint fk_contract_2_contract_t foreign key (contract_type) references contract_type (code);

alter table contract
  add constraint fk_contract_2_ma_deal foreign key (ma_deal_id) references master_agreement_deal (id);

alter table contract
  add constraint fk_contract_2_import_source foreign key (import_source) references import_source (code);

alter table contract
  add constraint fk_contract_2_copyright_type foreign key (copyright_type) references copyright_type(code);

-- Can't put unique constraint on (cw_id, number, source_id) because number
-- is a nullable field so use triggers instead
-- alter table contract add constraint un_contract unique (cw_id, number, source_id);

delimiter $$
create trigger contract_i_unique before insert on contract
for each row begin
  -- (declare inside if does not work)
  declare msg varchar(500);
  if (new.number is not null and exists(select id from contract where number = new.number and cw_id = new.cw_id and source_id = new.source_id)) then
    -- for some reason setting message_text directly does not work with concat
    set msg = concat('Duplicate number [', new.number, '] not allowed in contract');
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = msg;
  end if;
end;$$
delimiter ;

delimiter $$
create trigger contract_u_unique before update on contract
for each row begin
  -- (declare inside if does not work)
  declare msg varchar(500);
  if (new.number is not null and new.number != old.number and exists(select id from contract where number = new.number and cw_id = new.cw_id and source_id = new.source_id)) then
    -- for some reason setting message_text directly does not work with concat
    set msg = concat('Duplicate number [', new.number, '] not allowed in contract');
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = msg;
  end if;
end;$$
delimiter ;

create table contract_file (
  id int not null auto_increment,
  constraint pk_contract_file primary key (id),
  contract_id int not null,
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  file_name nvarchar(255) not null,
  description nvarchar(1000) not null,
  mime_type nvarchar(100) not null,
  file_data blob(10111000) not null
);

alter table contract_file
  add constraint fk_ctfile_2_ct foreign key (contract_id) references contract (id);

create table contract_2_asset (
  contract_id int not null,
  asset_base_id int not null,
  price decimal(8,2),
  rfdeal_id int,
  credit_line nvarchar(1000),
  no_crop bool not null default false,
  no_bleed bool not null default false
);

alter table contract_2_asset
  add constraint pk_contr_2_asset primary key (contract_id, asset_base_id);

alter table contract_2_asset
  add constraint fk_c2a_2_contract foreign key (contract_id) references contract (id);

alter table contract_2_asset
  add constraint fk_c2a_2_assetb foreign key (asset_base_id) references asset_base (id);

alter table contract_2_asset
  add constraint fk_c2a_rfdeal foreign key (rfdeal_id) references royalty_free_deal (id);

create index idx_c2a_contract_id on contract_2_asset (contract_id);
create index idx_c2a_abase_id on contract_2_asset (asset_base_id);

create table comp_copy (
  id int not null auto_increment,
  constraint pk_comp_copy primary key (id),
  contract_id int not null,
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  num_copies int not null,
  address_id int,
  order_num nvarchar(20),
  success_flag nchar(2),
  error_msg nvarchar(200)
);

alter table comp_copy
  add constraint fk_cc_2_contract foreign key (contract_id) references contract (id);

create table payment_request (
  id int not null auto_increment,
  constraint pk_payment_request primary key (id),
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  last_updated_user_id int not null,
  created_user_id int not null,
  number nvarchar(50),
  contract_id int not null,
  -- can date_submitted be removed and just use created_date?
  date_submitted datetime(3) not null,
  is_paid bool not null default false,
  cost_center nvarchar(20),
  account_id int
);

alter table payment_request
  add constraint fk_pr_2_user_u foreign key (last_updated_user_id) references user_table (id);

alter table payment_request
  add constraint fk_pr_2_user_c foreign key (created_user_id) references user_table (id);

alter table payment_request
  add constraint fk_pr_2_contract foreign key (contract_id) references contract (id);

alter table payment_request
  add constraint fk_pr_2_acct foreign key (account_id) references account (id);

-- prevent accidentally creating two payment requests for the same contract
alter table payment_request add constraint un_pr_contract_id unique (contract_id);

create table amendment (
  id int not null auto_increment,
  constraint pk_amendment primary key (id),
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  last_updated_user_id int not null,
  created_user_id int not null,
  contract_id int not null,
  note nvarchar(200),
  -- smarkoff: It would be best if these columns (file_name, mime_type, data) where not null
  -- but the way the code currently works is to create the record and then update it.
  -- Ideally should not be this way because when an error happens we get bad data.
  file_name nvarchar(200),
  mime_type nvarchar(100),
  data blob(10111000),
  language_code nvarchar(10)
);

alter table amendment
  add constraint fk_am_2_user_u foreign key (last_updated_user_id) references user_table (id);

alter table amendment
  add constraint fk_am_2_user_c foreign key (created_user_id) references user_table (id);
  
alter table amendment
  add constraint fk_am_2_ct foreign key (contract_id) references contract (id);

alter table amendment 
  add constraint fk_amend_2_lang foreign key (language_code) references enum_language (code);

-- prevent accidentally creating two payment requests for the same contract
alter table amendment add constraint un_amendment_contract_id unique (contract_id);

create table permission_status (
  code nvarchar(50) not null,
  constraint pk_perm_status primary key (code),
  description nvarchar(100)
);

create table page_pos_enum (
  code nvarchar(20) not null,
  constraint pk_page_pos_enum primary key (code),
  description nvarchar(100),
  sort_order int not null
);

create table asset_use (
  id int not null auto_increment,
  constraint pk_asset_use primary key (id),
  external_id nvarchar(64) not null,
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  last_updated_user_id int not null,
  created_user_id int not null,
  asset_id int not null,
  cw_id int not null,
  position nvarchar(64),
  component_id int,
  usage_type nvarchar(20) not null,
  manuscript_page nvarchar(8),
  final_page nvarchar(30),
  found_on nvarchar(200),
  is_color bool not null default false,
  size nvarchar(20) not null default 'N/A',
  permission_status nvarchar(50),
  status_explanation nvarchar(1000),
  last_updated_status datetime(3) not null,
  need_payment_request bool not null default false,
  paid bool not null default false,
  permission_comment nvarchar(5000) default null,
  production_comment nvarchar(5000) default null,
  request_comment nvarchar(2048) default null,
  caption nvarchar(500),
  camera_copy_to_come bool not null default false,
  is_canceled bool not null default false,
  is_removed bool not null default false,
  cancel_timestamp datetime(3) null,
  cancel_user_id int null,
  cancel_replacement_id int null,
  cancel_comment nvarchar(1000) default null,
  media_return_req bool not null default false,
  reused_from_prev_ed bool not null default false,
  reused_isbn nvarchar(13),
  reused_comment nvarchar(500),
  reused_position nvarchar(50),
  reused_page nvarchar(8),
  is_pickup bool not null default false,
  pickup_isbn nvarchar(13),
  pickup_comment nvarchar(500),
  pickup_position nvarchar(50),
  pickup_page nvarchar(8),
  pickup_issue_number nvarchar(20) default null,
  pickup_issue_date datetime(3) default null,
  pickup_title nvarchar(300) default null,
  pickup_author nvarchar(300) default null,
  is_r_use_approved bool not null default false,
  r_use_approved_user_id int,
  r_use_approved_date datetime(3),
  print_run_warning bool not null default false,
  new bool not null default false,
  page_pos nvarchar(20),
  wizard_owner_type nvarchar(30) default null,
  sent_to_production bool not null default false,
  is_custom bool not null default false,
  estimated_cost decimal(8,2) not null default 0,
  estimated_currency nvarchar(20),
  date_to_prod datetime(3),
  sort_order nvarchar(10),
  user_group_id int default null,
  previous_wileypub_au_id int,
  image_posted_ftp bool not null default false,
  pickup_edition_number int,
  is_page_proof bool not null default false,
  gbpm_category int null,
  import_source int null
);

alter table asset_use
  add constraint un_ause_ext_id unique (external_id);

alter table asset_use
  add constraint fk_au_2_curr foreign key (estimated_currency) references currency (code);

alter table asset_use
  add constraint fk_ause_2_asset foreign key (asset_id) references asset (id);
  
alter table asset_use
  add constraint fk_ause_2_cw foreign key (cw_id) references common_work (id);
  
create index idx_ause_asset_id on asset_use (asset_id);
create index idx_ause_cw_id on asset_use (cw_id);

alter table asset_use
  add constraint fk_ause_2_comp foreign key (component_id) references component (id);

alter table asset_use
  add constraint fk_ause_2_usage foreign key (usage_type) references usage_type (code);

alter table asset_use
  add constraint fk_ause_2_ps foreign key (permission_status) references permission_status (code);

alter table asset_use
  add constraint fk_ause_2_se foreign key (size) references size_enum (code);

alter table asset_use
  add constraint fk_au_2_user_u foreign key (last_updated_user_id) references user_table (id);

alter table asset_use
  add constraint fk_au_2_user_c foreign key (created_user_id) references user_table (id);
  
alter table asset_use
  add constraint fk_ause_2_ppos_enum foreign key (page_pos) references page_pos_enum (code);

alter table asset_use
  add constraint fk_ause_2_user2 foreign key (r_use_approved_user_id) references user_table (id);

alter table asset_use
  add constraint fk_asset_use_2_user_group foreign key (user_group_id) references user_group (id);

alter table asset_use
  add constraint fk_asset_use_2_previous foreign key (previous_wileypub_au_id) references asset_use (id) ON DELETE SET NULL;

alter table asset_use
  add constraint fk_asset_use_2_repl_id foreign key (cancel_replacement_id) references asset_use (id) ON DELETE SET NULL;

alter table asset_use
  add constraint fk_ause_2_gbpmcat foreign key (gbpm_category) references gbpm_category (code);

alter table asset_use
  add constraint fk_ause_2_import_source foreign key (import_source) references import_source (code);

create table au_source_perm_status (
    asset_use_id int not null,
    source_id int not null,
    permission_status nvarchar(50) not null,
    status_explanation nvarchar(1000),
    latest_contract_id int,
    latest_po_id int,
    active_contract_id int,
    active_po_id int,
    contract_credit_line nvarchar(1000),
    need_payment_request bool not null default false,
    paid bool not null default false,
    last_updated_date datetime(3) not null
);

alter table au_source_perm_status
  add constraint pk_au_source_ps primary key (asset_use_id, source_id);

alter table au_source_perm_status
  add constraint fk_ausps_2_au foreign key (asset_use_id) references asset_use (id) ON DELETE CASCADE;

alter table au_source_perm_status
  add constraint fk_ausps_2_source foreign key (source_id) references source (id);

alter table au_source_perm_status
  add constraint fk_ausps_2_ps foreign key (permission_status) references permission_status (code);

alter table au_source_perm_status
  add constraint fk_ausps_2_l_contract foreign key (latest_contract_id) references contract (id) ON DELETE CASCADE;

alter table au_source_perm_status
  add constraint fk_ausps_2_a_contract foreign key (active_contract_id) references contract (id) ON DELETE CASCADE;

alter table au_source_perm_status
  add constraint fk_ausps_2_l_po foreign key (latest_po_id) references purchase_order (id) ON DELETE CASCADE;

alter table au_source_perm_status
  add constraint fk_ausps_2_a_po foreign key (active_po_id) references purchase_order (id) ON DELETE CASCADE;

create table condition_type (
  code nvarchar(50) not null,
  constraint pk_cond_type primary key (code),
  description nvarchar(100) not null,
  parent_code nvarchar(50),
  data_type nvarchar(20) default null,
  sort_order int not null,
  validation_class nvarchar(100) default null,
  can_see_in_cw bool not null default false,
  can_edit_in_cw bool not null default false,
  can_see_in_ct bool not null default true,
  can_edit_in_ct bool not null default true,
  can_see_in_madeal bool not null default false,
  can_edit_in_madeal bool not null default false
);

alter table condition_type
  add constraint fk_cond_type_2_self foreign key (parent_code) references condition_type (code);

alter table condition_type
  add constraint fk_cond_type_2_dt foreign key (data_type) references data_type (code);

-- 'condition' is a reserved word in MySQL
create table condition_value (
  id int not null auto_increment,
  constraint pk_condition primary key (id),
  condition_type nvarchar(50) not null,
  value nvarchar(2000),
  rollup_value nvarchar(2000)
);

alter table condition_value
  add constraint fk_cond_value_2_cond_type foreign key (condition_type) references condition_type (code) on update cascade;

create table cw_2_condition (
  cw_id int not null,
  condition_id int not null
);

-- smarkoff: Alternatively could not have a primary key but make condition_id unique
alter table cw_2_condition
  add constraint pk_cw_2_condition primary key (condition_id);

alter table cw_2_condition
  add constraint fk_cw2c_2_cw foreign key (cw_id) references common_work (id);

alter table cw_2_condition
  add constraint fk_cw2c_2_cond foreign key (condition_id) references condition_value (id);

create index idx_cw2c_cw_id on cw_2_condition (cw_id);

create table contract_2_condition (
  contract_id int not null,
  condition_id int not null
);

alter table contract_2_condition
  add constraint pk_contract_2_cond primary key (condition_id);

alter table contract_2_condition
  add constraint fk_c2c_2_contract foreign key (contract_id) references contract (id);

alter table contract_2_condition
  add constraint fk_c2c_2_cond foreign key (condition_id) references condition_value (id);

create index idx_c2c_contract_id on contract_2_condition (contract_id);

create table ma_deal_2_condition (
  ma_deal_id int not null,
  condition_id int not null
);

alter table ma_deal_2_condition
  add constraint pk_ma_deal_2_cond primary key (condition_id);

alter table ma_deal_2_condition
  add constraint fk_mad2c_2_mad foreign key (ma_deal_id) references master_agreement_deal (id);

alter table ma_deal_2_condition
  add constraint fk_mad2c_2_cond foreign key (condition_id) references condition_value (id);

create index idx_mad2c_mad_id on ma_deal_2_condition (ma_deal_id);

create table msg_type (
  code nvarchar(20) not null,
  constraint pk_msg_type primary key (code),
  description nvarchar(100)
);

create table msg_operation_type (
  code nvarchar(50) not null,
  constraint pk_msg_op_type primary key (code),
  description nvarchar(100)
);

create table msg_status (
  code nvarchar(50) not null,
  constraint pk_msg_status primary key (code),
  description nvarchar(100)
);

create table cached_msg (
    id int not null auto_increment,
    constraint pk_cached_msg primary key (id),
    message_id nvarchar(50) not null,
    status nvarchar(50) not null,
    direction nvarchar(20) not null,
    type nvarchar(20) not null,
    operation_type nvarchar(50) not null,
    operation_sub_type nvarchar(50),
    source nvarchar(20) not null,
    item_count int not null,
    metadata_count int not null,
    delivery_attempts int not null default 0,
    last_delivery_attempt datetime(3) not null,
    failure_message text(1222000),
    message_xml text(50111000) not null,
    external_xml text(50111000) default null,
    sent datetime,
	created_date datetime(3) not null,
	last_updated_date datetime(3) not null,
	protocol_version nvarchar(20) not null default '1.0'
);

alter table cached_msg
  add constraint fk_msg_2_type foreign key (type) references msg_type (code);

alter table cached_msg
  add constraint fk_msg_2_op_type foreign key (operation_type) references msg_operation_type (code);

alter table cached_msg
  add constraint fk_msg_2_status foreign key (status) references msg_status (code);

create table cached_msg_target (
    id int not null auto_increment,
    constraint pk_cached_msg_trgt primary key (id),
    cached_msg_id int not null,
    target nvarchar(20) not null
);

alter table cached_msg_target
    add constraint fk_cached_msg_trgt foreign key (cached_msg_id) references cached_msg(id)
    on delete cascade;

create table product_printing (
  id int not null auto_increment,
  constraint pk_product_print primary key (id),
  product_id int not null,
  printing_number int not null,
  distribution_center int not null,
  po_number nvarchar(16) not null,
  po_status nvarchar(50) not null,
  po_date datetime(3),
  order_quantity int not null,
  expected_delivery_date datetime(3),
  received_date datetime(3),
  received_quantity int not null,
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null
);

alter table product_printing
  add constraint fk_printing_2_p foreign key (product_id) references product (id);

alter table product_printing
  add constraint un_prod_printing unique (product_id, printing_number, distribution_center, po_number);

create index idx_printing_p_id on product_printing (product_id);

create table user_profile (
  id int not null auto_increment,
  constraint pk_user_profile primary key (id),
  user_id int not null,
  name nvarchar(50) not null,
  title nvarchar(100) not null,
  visible bool not null default false,
  required bool not null default false,
  sortable bool not null default false,
  searchable bool not null default false,
  editable bool not null default false,
  width int not null default 0,
  sort_order int not null default 0,
  sort_type nvarchar(25) default null
);

alter table user_profile
  add constraint fk_user_prof_2_user foreign key (user_id) references user_table (id);

alter table user_profile
  add constraint un_user_profile unique (user_id, name);

create table user_2_source (
  user_id int not null,
  source_id int not null
);

alter table user_2_source
  add constraint pk_user_2_source  primary key (user_id, source_id);

alter table user_2_source
  add constraint fk_u2s_2_user foreign key (user_id) references user_table (id);

alter table user_2_source
  add constraint fk_u2s_2_source foreign key (source_id) references source (id);

create index idx_u2s_user_id on user_2_source (user_id);


create table system_notification (
  id int not null auto_increment,
  constraint pk_notification primary key (id),
  from_date datetime(3) not null,
  to_date datetime(3) not null,
  message_to_post nvarchar(1000)
);

create table cw_history (
	id int not null auto_increment,
	constraint pk_cw_history primary key (id),
	cw_id int not null,
	created_date datetime(3) not null,
	last_updated_date datetime(3) not null,
	last_updated_user_id int not null,
	created_user_id int not null,
	description nvarchar(1000)
);

alter table cw_history
  add constraint fk_cw_hist_2_cw foreign key (cw_id) references common_work (id);

alter table cw_history
  add constraint fk_cw_hist_2_user_u foreign key (last_updated_user_id) references user_table (id);
  
alter table cw_history
  add constraint fk_cw_hist_2_user_c foreign key (created_user_id) references user_table (id);

delimiter $$
create trigger history_au_i after insert on asset_use
for each row begin
  declare rowCount int;
  set rowCount = (select count(*) from cw_history where cw_id = new.cw_id
	and last_updated_user_id = new.last_updated_user_id
	-- for some reason description = 'Asset(s) added/modified' does not match so use like
	and description like 'Asset% added/modified'
	and datediff(last_updated_date, current_timestamp) = 0);
  if (rowCount = 0) then
    insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
      values (new.cw_id, current_timestamp, current_timestamp, new.last_updated_user_id, new.created_user_id, 'Asset(s) added/modified');
  end if;
end;$$
delimiter ;

-- use code for asset_use delete because won't know user_id otherwise

-- Should do same as above, but in only one statement
-- delimiter $$
-- create trigger history_au_i after insert on asset_use
-- for each row begin
--   insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
--   select new.cw_id, current_timestamp, current_timestamp, new.last_updated_user_id, new.created_user_id, 'Asset(s) added/modified'
--   from asset_use
--   where new.cw_id not in (select cw_id from cw_history where cw_id = new.cw_id
--       and new.last_updated_user_id = last_updated_user_id
       -- for some reason description = 'Asset(s) added/modified' does not match so use like
-- 	   and description like 'Asset% added/modified'
--	   and datediff(last_updated_date, current_timestamp) = 0
--   );
-- end;$$
-- delimiter ;

delimiter $$
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
end;$$
delimiter ;

delimiter $$
create trigger history_asset_u after update on asset
for each row begin
  insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
  select distinct au.cw_id, current_timestamp, current_timestamp, ab.last_updated_user_id, ab.created_user_id, 'Asset(s) added/modified'
  from asset a, asset_base ab, asset_use au
    where ab.id = a.id
    and a.id = new.id and au.asset_id = a.id
    and au.cw_id not in (select cw_id from cw_history where cw_id = au.cw_id
      and ab.last_updated_user_id = last_updated_user_id
      -- for some reason description = 'Asset(s) added/modified' does not match so use like
  	  and description like 'Asset% added/modified'
	  and datediff(last_updated_date, current_timestamp) = 0
	 );
end;$$
delimiter ;

delimiter $$
create trigger perm.history_po_i after insert on perm.purchase_order
for each row begin  
insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
values (new.cw_id, current_timestamp, current_timestamp, new.last_updated_user_id, new.created_user_id, 
concat((CASE WHEN new.is_permission_request = false THEN 'Waiting on Invoice ' ELSE 'Form sent to ' END),
(select name from source where id = new.source_id)));
end;$$
delimiter ;

delimiter $$
create trigger history_contract_i after insert on contract
for each row begin
  insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
    values (new.cw_id, current_timestamp, current_timestamp, new.last_updated_user_id, new.created_user_id, 'Details entered for '
      || (select name from source where id = new.source_id));
end;$$
delimiter ;

delimiter $$
create trigger history_pay_req_i after insert on payment_request
for each row begin
  insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
    values ((select cw_id from contract c where c.id = new.contract_id), current_timestamp, current_timestamp,
    	new.last_updated_user_id, new.created_user_id, 'Payment request created for '
      || (select s.name from source s, contract c where c.id = new.contract_id and s.id = c.source_id));
end;$$
delimiter ;

delimiter $$
create trigger history_cw_i after insert on common_work
for each row begin
  insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
    -- change "1, 1" to "new.last_updated_user_id, new.created_user_id" after add these columns to common_work table
    -- (for now not bothering because James says not important for cw table)
    values (new.id, current_timestamp, current_timestamp, 1, 1, 'Project created');
end;$$
delimiter ;

create table pe_update (
  object_type nvarchar(20) not null,
  data_source nvarchar(20) not null,
  last_update datetime(3) not null
);

alter table pe_update
  add constraint fk_pe_update_2_data_source foreign key (data_source) references data_source (code);

alter table pe_update
  add constraint un_pe_update unique (object_type, data_source);

create table asset_perm_ref (
	id int not null auto_increment,
	constraint pk_asset_perm_ref primary key (id),
	asset_id int not null,
    asset_use_id int not null,
    cw_id int not null,
    source_id int,
    po_id int,
    contract_id int,
    payment_request_id int,
    permission_status nvarchar(50),
    status_explanation nvarchar(1000)
);

alter table asset_perm_ref
  add constraint un_asset_perm_ref unique (asset_use_id, source_id);

alter table asset_perm_ref
  add constraint fk_apr_2_a foreign key (asset_id) references asset (id);

alter table asset_perm_ref
  add constraint fk_apr_2_au foreign key (asset_use_id) references asset_use (id) ON DELETE CASCADE;

alter table asset_perm_ref
  add constraint fk_apr_2_cw foreign key (cw_id) references common_work (id);

alter table asset_perm_ref
  add constraint fk_apr_2_s foreign key (source_id) references source (id);

alter table asset_perm_ref
  add constraint fk_apr_2_c foreign key (contract_id) references contract (id) ON DELETE CASCADE;

alter table asset_perm_ref
  add constraint fk_apr_2_po foreign key (po_id) references purchase_order (id) ON DELETE CASCADE;

alter table asset_perm_ref
  add constraint fk_apr_2_pr foreign key (payment_request_id) references payment_request (id);

alter table asset_perm_ref
  add constraint fk_apr_2_ps foreign key (permission_status) references permission_status (code);

create table source_2_favorite_group (
  favorite_group_id int not null,
  source_id int not null
);

alter table source_2_favorite_group
  add constraint pk_source_2_fav  primary key (favorite_group_id, source_id);

alter table source_2_favorite_group
  add constraint fk_s2f_2_source foreign key (source_id) references source (id);

alter table source_2_favorite_group
  add constraint fk_s2f_2_favorite_group foreign key (favorite_group_id) references favorite_group (id);

create table author_2_component (
  user_id int not null,
  component_id int not null,
  cw_id int not null
);

alter table author_2_component
  add constraint pk_author_2_cw primary key (user_id, component_id);

alter table author_2_component
  add constraint fk_author2ccomp_2_user foreign key (user_id) references user_table (id);

alter table author_2_component
  add constraint fk_author2ccomp_2_comp foreign key (component_id) references component (id);

alter table author_2_component
  add constraint fk_author2ccomp_2_cw foreign key (cw_id) references common_work (id);

create index idx_author2comp_cw_id on author_2_component (cw_id);
create index idx_author2comp_user_id on author_2_component (user_id);
create index idx_author2comp_comp_id on author_2_component (component_id);

-- this table is for associating an author to a cw separate from any PE data (for the author interface)
create table author_2_cw (
  user_id int not null,
  cw_id int not null,
  due_date datetime(3) default null,
  read_only bool not null default false,
  permission_complete bool not null default false,
  created_user_id int default 1
);

alter table author_2_cw
  add constraint pk_author_2_cw primary key (user_id, cw_id);

alter table author_2_cw
  add constraint fk_author2cw_2_user foreign key (user_id) references user_table (id);

alter table author_2_cw
  add constraint fk_author2cw_2_cw foreign key (cw_id) references common_work (id);

alter table author_2_cw 
  add constraint fk_author_2_cw_c foreign key (created_user_id) references user_table (id);

create index idx_author2cw_u_id on author_2_cw (user_id);
create index idx_author2cw_cw_id on author_2_cw (cw_id);

delimiter $$
create trigger history_a_2_cw after update on author_2_cw
for each row begin
	if new.permission_complete = 0 and new.permission_complete != old.permission_complete then	
	    insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
	      values (new.cw_id, current_timestamp, current_timestamp, new.user_id, new.user_id, 'Permission complete flag has been removed');
	end if;
	if new.permission_complete = 1 and new.permission_complete != old.permission_complete then
	    insert into cw_history(cw_id, created_date, last_updated_date, last_updated_user_id, created_user_id, description)
	      values (new.cw_id, current_timestamp, current_timestamp, new.user_id, new.user_id, 'Permission complete flag has been set');		
  	end if;
end;$$
delimiter ;

create table user_2_privilege (
  user_id int not null,
  privilege_code nvarchar(20) not null,
  cw_id int
);

-- can't have primary key including cw_id since nullable but can have unique constraint
alter table user_2_privilege
  add constraint un_user_2_priv unique (user_id, privilege_code, cw_id);

alter table user_2_privilege
  add constraint fk_up_2_user foreign key (user_id) references user_table (id);

alter table user_2_privilege
  add constraint fk_up_2_privilege foreign key (privilege_code) references privilege (code);

alter table user_2_privilege
  add constraint fk_up_2_cw foreign key (cw_id) references common_work (id);

create table author_default_privilege (
  -- this is the user_id of an internal user, not an author
  user_id int not null,
  privilege_code nvarchar(20) not null
);

alter table author_default_privilege add constraint primary key (user_id, privilege_code);

alter table author_default_privilege
  add constraint fk_adp_2_user foreign key (user_id) references user_table (id);

alter table author_default_privilege
  add constraint fk_adp_2_privilege foreign key (privilege_code) references privilege (code);


-- These views are just for convenience - not used in the code at all

create view view_cw_condition
as select map.cw_id, cv.* from cw_2_condition map, condition_value cv where map.condition_id = cv.id;

create view view_contract_condition
as select map.contract_id, cv.* from contract_2_condition map, condition_value cv where map.condition_id = cv.id;

create view view_ma_deal_condition
as select map.ma_deal_id, cv.* from ma_deal_2_condition map, condition_value cv where map.condition_id = cv.id;

create view view_user_2_role
as select u.code as u_code, u.first_name, u.last_name, r.code, r.role_type, r.is_global, map.product_id
from user_table u, role r, user_2_role map
where r.id = map.role_id and u.id = map.user_id order by last_name desc;

-- cannot have foreign key because a source can be transfered to another source which can also be transfered later to another source etc
-- The logic here is that when the new source id is not in the transfers table (with new_source_id = original_source_id), then the new source_id
-- can be used to find the source in the source table.  If the new source id is also found as a origninal_source_id, then this source
-- has also been merged and will not be found in the source table.

create table source_transfer_history (
  id int not null auto_increment,
    constraint pk_source primary key (id),
  original_source_id int not null,
  original_external_id nvarchar(64) not null,
  original_external_name nvarchar(200) not null,
  new_external_name nvarchar(200) not null,
  current_source_id int not null,
  update_user_id int not null,
  created_date datetime(3) not null
);

create table asset_use_file (
  id int not null auto_increment,
  constraint pk_asset_use_file primary key (id),
  au_id int not null,
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  file_name nvarchar(200) not null,
  description nvarchar(200),
  mime_type nvarchar(100) not null,
  file_data blob(10111000) not null
);

alter table asset_use_file
  add constraint fk_aufile_2_au foreign key (au_id) references asset_use (id);

create table product_index_file_position (
  position int not null
);

create table guideline_file (
  id int not null auto_increment,
  constraint pk_guideline_file primary key (id),
  created_date datetime(3) not null,
  last_updated_date datetime(3) not null,
  display_name nvarchar(200) not null,
  file_name nvarchar(200) not null,
  mime_type nvarchar(100) not null,
  file_data blob(10111000) not null,
  sort_order int not null
);

alter table guideline_file
  add constraint un_gf_display_name unique (display_name);

-- we may be able to delete this table later -- just has one column (asset_id) int not null
create table import_ignore select asset_id from (select distinct asset_id, cw_id from asset_use where import_source=1)t  
group by asset_id having count(*) >=2;

create table enum_cancel_reason (
  code nvarchar(20) not null,
  constraint pk_enum_cancel_reason primary key (code),
  description nvarchar(100)
);

create table enum_usage_condition (
  code nvarchar(20) not null,
  constraint pk_enum_usage_condition primary key (code),
  description nvarchar(100),
  sort_order int not null
);

-- This table is sort of an extension of the contract_2_asset table
create table usage_2_size (
  contract_id int not null,
  asset_base_id int not null,
  usage_condition_code nvarchar(20),
  size_code nvarchar(20) not null
);

-- We won't bother to create a primary key for this table because really it would be a composite key of all 4 columns,
-- but since usage_condition_code is nullable we can't actually include it in a primary key.
-- So leave out the primary key -- don't need it since we will not create a JPA bean for this table.

alter table usage_2_size
  add constraint fk_u2size_2_c2asset foreign key (contract_id, asset_base_id) references contract_2_asset (contract_id, asset_base_id)
  on delete cascade;

alter table usage_2_size
  add constraint fk_u2size_2_usage_cond foreign key (usage_condition_code) references enum_usage_condition (code);

alter table usage_2_size
  add constraint fk_u2size_2_size foreign key (size_code) references size_enum (code);

---- STATUS SECTION ----

---- 1 - 264 merged with this
-- (merged with create script above and/or seed script now)

---- Machine status

-- Dev: run through 230-264 (last copied from prod 6/20/2014)
-- QA: run through 230-264 (last copied from prod 6/20/2014)
-- Prod: run through 230-263
