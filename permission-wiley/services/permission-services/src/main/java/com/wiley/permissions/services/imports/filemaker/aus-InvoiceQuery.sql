select 
 id,
 project_No,
 SourceExternalId,  
 PO_number,
 po_date,
 Po_Amount,
 Invoice_Number,
 Invoice_Date,
 Invoice_Amount,
 date_invoice_payed,
 status,
 error,
 source_name 
from ozInvoicelist 
where status = 'success [0]' and source_name <> ''
order by project_no, sourceExternalId, po_number, invoice_number