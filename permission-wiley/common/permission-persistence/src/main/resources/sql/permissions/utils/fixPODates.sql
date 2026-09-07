-- smarkoff: ran this again 11/12/2013 on all environments and QA + PROD needed some CWs to be corrected (listed below)
-- (QA and PROD needed the same ones since QA was copied from PROD)

select distinct c.cw_id from contract c join purchase_order po on c.purchase_order_id = po.id 
where c.date < po.date;
/*
http://permissionprod.wiley.com:63082/service/asset/recalculate/cw/46966,2615,117399,176697,54953,2677,123684,122482,124539,1391,211932,106316,114828,112958,119624,119263,27091,123981,63030,129568,124370,1384,28006,129299,181102,122897,125737,111349,102220,179385,99833,198,1297,3978
*/

select po.date, c.date from contract c join purchase_order po on c.purchase_order_id = po.id 
where c.date < po.date;

update purchase_order po join contract c on c.purchase_order_id = po.id 
set po.date = c.date where c.date < po.date;
