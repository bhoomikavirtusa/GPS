-- merged with create

alter table contract ADD copyright_type nvarchar(20) NOT NULL DEFAULT '0';

alter table contract ADD CONSTRAINT fk_contract_2_copyright_type FOREIGN KEY (copyright_type) REFERENCES copyright_type(code);
