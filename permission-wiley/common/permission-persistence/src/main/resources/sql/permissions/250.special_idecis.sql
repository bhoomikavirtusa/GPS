-- merged with create
ALTER TABLE source ADD INDEX (name(100) );

-- You may need to run this first before adding the FK below
-- If you already added an index
--alter table contact drop index source_id;

-- clean up orphaned contacts
delete from contact where source_id not in (select id from source);

alter table contact
  add constraint fk_contact_2_source foreign key (source_id) references source (id);

-- We do not need an index on source_2_address.source_id -- already a foreign key on this column
