DROP TABLE ozInvoicelist CASCADE;
CREATE TABLE ozInvoicelist
(
   id                  int             NOT NULL,
  project_No          varchar(30),
  SourceExternalId    varchar(30),
   PO_number          varchar(30),
   PO_date            varchar(20),
  Po_Amount        	  varchar(20),
  Invoice_Number      varchar(30),
  Invoice_Date        varchar(20),
  Invoice_Amount      varchar(20),
 	date_invoice_payed  varchar(20),
  status              varchar(20),
  error          	    varchar(1000),
  source_name          varchar(500)
)
ENGINE=InnoDB;

ALTER TABLE ozInvoicelist
   ADD CONSTRAINT pk_ozinvoicelist
   PRIMARY KEY (id);


