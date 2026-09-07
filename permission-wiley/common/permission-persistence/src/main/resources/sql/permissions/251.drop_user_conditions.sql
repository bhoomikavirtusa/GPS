-- merged with create

drop view view_user_condition;
drop table user_2_condition;

delete from condition_value where id not in (select condition_id from contract_2_condition)
and id not in (select condition_id from po_2_condition)
and id not in (select condition_id from ma_deal_2_condition)
and id not in (select condition_id from cw_2_condition);
