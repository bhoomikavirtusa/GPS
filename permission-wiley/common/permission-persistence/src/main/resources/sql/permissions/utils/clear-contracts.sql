delete from contract_file where contract_id not in (select distinct contract_id from contract_2_asset);

delete from contract where id not in (select distinct contract_id from contract_2_asset);