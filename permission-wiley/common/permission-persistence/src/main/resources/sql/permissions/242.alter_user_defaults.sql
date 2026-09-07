-- merged with create

update user_defaults set date_format = 'mm/dd/yyyy' where date_format is null;

alter table user_defaults modify column date_format nvarchar(20) not null default 'mm/dd/yyyy';
