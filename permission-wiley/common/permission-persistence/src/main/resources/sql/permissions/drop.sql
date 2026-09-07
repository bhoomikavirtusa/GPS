---------- Triggers
-- (triggers dropped automatically with tables in MySQL)

---------- Views
drop VIEW view_contract_condition;
drop VIEW view_cw_condition;
drop VIEW view_ma_deal_condition;
drop VIEW view_user_2_role;

---------- Triggers
drop trigger contract_i_unique;
drop trigger contract_u_unique;
drop trigger history_asset_u;
drop trigger history_au_i;
drop trigger history_au_u;
drop trigger history_a_2_cw;
drop trigger history_contract_i;
drop trigger history_cw_i;
drop trigger history_pay_req_i;
drop trigger history_po_i;
drop trigger user_i_email_unique;
drop trigger user_u_email_unique;

---------- (no procedures to drop)

---------- (no functions to drop)

---------- (no sequences to drop)

---------- Tables
drop TABLE amendment;
drop TABLE asset_2_source;
drop TABLE asset_file;
drop TABLE asset_perm_ref;
drop TABLE asset_use_file;
drop TABLE au_source_perm_status;
drop TABLE author_2_component;
drop TABLE author_2_cw;
drop TABLE author_default_privilege;
drop TABLE cached_msg_target;
drop TABLE comp_copy;
drop TABLE contact;
drop table usage_2_size;
drop TABLE contract_2_asset;
drop TABLE contract_2_condition;
drop TABLE contract_file;
drop TABLE cw_2_condition;
drop TABLE cw_file;
drop TABLE cw_history;
drop TABLE cw_photo_estimate;
drop TABLE cw_summary;
drop TABLE editor;
drop TABLE guideline_file;
drop TABLE ma_deal_2_condition;
drop TABLE madeal_2_businessunit;
drop TABLE madeal_2_ulocation;
drop TABLE payment_request;
drop TABLE pe_update;
drop TABLE photo_estimate_type;
drop TABLE product_2_bundle;
drop TABLE product_index_file_position;
drop TABLE product_printing;
drop TABLE purchase_order_2_asset;
drop TABLE relation;
drop TABLE relation_code;
drop TABLE rendition_type;
drop TABLE royalty_free_deal;
drop TABLE source_2_address;
drop TABLE source_2_favorite_group;
drop TABLE source_file;
drop TABLE source_transfer_history;
drop TABLE system_notification;
drop TABLE user_2_privilege;
drop TABLE user_2_role;
drop TABLE user_2_source;
drop TABLE user_defaults;
drop TABLE user_location;
drop TABLE user_profile;
drop TABLE watched_cw;
drop TABLE wiley_entity;
drop TABLE account;
drop TABLE address;
drop TABLE address_type;
drop TABLE asset_use;
drop TABLE bundle;
drop TABLE cached_msg;
drop TABLE component;
drop TABLE component_category;
drop TABLE condition_value;
drop TABLE contract;
drop TABLE contract_type;
drop TABLE country;
drop TABLE master_agreement_deal;
drop TABLE msg_operation_type;
drop TABLE msg_status;
drop TABLE msg_type;
drop TABLE page_pos_enum;
drop TABLE permission_status;
drop TABLE privilege;
drop TABLE product;
drop TABLE product_edition;
drop TABLE product_family;
drop TABLE product_line;
drop TABLE product_type;
drop TABLE publication_status;
drop TABLE purchase_order;
drop TABLE role;
drop TABLE role_type;
drop TABLE size_enum;
drop TABLE source;
drop TABLE source_group;
drop TABLE subject_code;
drop TABLE submedium;
drop TABLE usage_type;
drop TABLE asset;
drop TABLE asset_group;
drop TABLE business_unit;
drop TABLE common_work;
drop TABLE common_work_status;
drop TABLE condition_type;
drop TABLE copyright_type;
drop TABLE currency;
drop TABLE data_source;
drop TABLE data_type;
drop TABLE delivery_method;
drop TABLE geo_location;
drop TABLE media_type;
drop TABLE medium;
drop TABLE model_release;
drop TABLE owner_type;
drop TABLE permission_payer;
drop TABLE asset_base;
drop TABLE user_table;
drop TABLE user_type;
drop TABLE user_group;
drop TABLE favorite_group;
drop table gbpm_category;
drop table import_ignore;
drop table import_source;
drop table enum_cancel_reason;
drop table enum_language;
drop table enum_usage_condition;
