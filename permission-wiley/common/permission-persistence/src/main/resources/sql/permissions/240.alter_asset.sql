-- merged with create

alter table asset add column is_gratis bool not null default false;
alter table asset add column is_stm_guidelines bool not null default false;
