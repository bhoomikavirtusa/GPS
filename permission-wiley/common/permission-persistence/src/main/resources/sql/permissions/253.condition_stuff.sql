-- merged with create and seed

-- Important!
-- run 231 (if not already run)
-- login to USDC Linux box (dev, qa, or prod) as smarkoff
-- convert4.bash connect-prod.properties 2>&1 | tee convert4.output.prod
-- (I suggest saving the output just in case of errors / future debugging)
-- This program is safe to run multiple times (but NOT script 231) if it is stopped in the middle for any reason.

-- convert4 will take 3-4 hours

-- cleanup
delete from contract_2_condition where condition_id in (select id from condition_value where condition_type in ('edition_pf_for_this'));
delete from ma_deal_2_condition where condition_id in (select id from condition_value where condition_type in ('edition_pf_for_this'));
delete from condition_value where condition_type in ('edition_pf_for_this');
delete from condition_type where code in ('edition_pf_for_this');


delete from contract_2_condition where condition_id in (select id from condition_value where condition_type in ('dwork_isv', 'dwork_custom'));
delete from ma_deal_2_condition where condition_id in (select id from condition_value where condition_type in ('dwork_isv', 'dwork_custom'));
delete from condition_value where condition_type in ('dwork_isv', 'dwork_custom');
delete from condition_type where code in ('dwork_isv', 'dwork_custom');


delete from contract_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_paper', 'medium_cloth', 'medium_cdrom'));
delete from ma_deal_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_paper', 'medium_cloth', 'medium_cdrom'));
delete from condition_value where condition_type in('medium_paper', 'medium_cloth', 'medium_cdrom');
delete from condition_type where code in ('medium_paper', 'medium_cloth', 'medium_cdrom');

delete from contract_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_all_future', 'medium_wiley_hosted', 'medium_cust_hosted'));
delete from ma_deal_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_all_future', 'medium_wiley_hosted', 'medium_cust_hosted')); 
delete from condition_value where condition_type in('medium_all_future', 'medium_wiley_hosted', 'medium_cust_hosted');
delete from condition_type where code in ('medium_all_future', 'medium_wiley_hosted', 'medium_cust_hosted');

delete from contract_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_ebook', 'medium_all_online_d'));
delete from ma_deal_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_ebook', 'medium_all_online_d'));
delete from condition_value where condition_type in('medium_ebook', 'medium_all_online_d');
delete from condition_type where code in ('medium_ebook', 'medium_all_online_d');

delete from contract_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_all_elect'));
delete from ma_deal_2_condition where condition_id in (select id from condition_value where condition_type in ('medium_all_elect'));
delete from condition_value where condition_type in('medium_all_elect');
delete from condition_type where code in ('medium_all_elect');

alter table condition_type drop column can_see_in_po;
alter table condition_type drop column can_edit_in_po;

update condition_value set value = null where value = 'true' and condition_type in ('language', 'sales', 'dwork', 'edition', 'medium', 'sublicense');

