insert into product_index_file_position (position) values (0);

insert into contract_type (code, description) values ('frontlist', 'Frontlist');
insert into contract_type (code, description) values ('reprint', 'Reprint');
insert into contract_type (code, description) values ('remediation', 'Remediation');

insert into address_type (code, description) values ('A', 'Main Address');
insert into address_type (code, description) values ('B', 'Billing Address');
insert into address_type (code, description) values ('M', 'Other Address');

insert into model_release (code, name) values ('acquired', 'Acquired');
insert into model_release (code, name) values ('not_needed', 'Not needed');
insert into model_release (code, name) values ('needed', 'Needed');

insert into common_work_status (code, description) values ('in_progress', 'In Progress');
insert into common_work_status (code, description) values ('no_perm_req', 'No Permissions Req.');
insert into common_work_status (code, description) values ('complete', 'Complete');

insert into publication_status (code, description) values ('Z', 'WTHDRWN FROM SALE No build set');
insert into publication_status (code, description) values ('X', 'No reprint');
insert into publication_status (code, description) values ('W', 'New edition pending');
insert into publication_status (code, description) values ('S', 'Suspended');
insert into publication_status (code, description) values ('R', 'PHY CNT IN PROGRESS-ultra shrt');
insert into publication_status (code, description) values ('P', 'Pre-contract');
insert into publication_status (code, description) values ('O', 'Out of Print');
insert into publication_status (code, description) values ('N', 'Published');
insert into publication_status (code, description) values ('K', 'KREIGER TITLE (DESTROY)');
insert into publication_status (code, description) values ('I', 'In Production');
insert into publication_status (code, description) values ('F', 'Under consideration sub custom');
insert into publication_status (code, description) values ('E', 'Editorial');
insert into publication_status (code, description) values ('C', 'Canceled');
insert into publication_status (code, description) values ('B', 'WITHDRAWN FROM SALE');
insert into publication_status (code, description) values ('A', 'MAY/MAY NOT REPRINT');

insert into permission_payer (code, description) values ('author', 'Author');
insert into permission_payer (code, description) values ('wiley', 'Wiley');

insert into permission_status (code, description) values ('noSource', 'Not Requested - No Source');
insert into permission_status (code, description) values ('missingInfo', 'Missing Info');
insert into permission_status (code, description) values ('unrequested', 'Not Requested');
insert into permission_status (code, description) values ('notRequestedBeWorkForHire', 'Not Requested - Will be Work for Hire');
insert into permission_status (code, description) values ('notRequestedBeRoyaltyFree', 'Not Requested - Will be Royalty Free');
insert into permission_status (code, description) values ('formSent', 'Request Sent');
insert into permission_status (code, description) values ('formSentBeWorkForHire', 'Request Sent - Will be Work for Hire');
insert into permission_status (code, description) values ('formSentBeRoyaltyFree', 'Request Sent - Will be Royalty Free');
insert into permission_status (code, description) values ('canceled', 'Cancelled');
insert into permission_status (code, description) values ('removed', 'Removed');
insert into permission_status (code, description) values ('replaced', 'Replaced');
insert into permission_status (code, description) values ('illegal', 'Error');
insert into permission_status (code, description) values ('amendmentNeeded', 'Limitation of Liability Needed');
insert into permission_status (code, description) values ('amendmentSent', 'Limitation of Liability Sent');
insert into permission_status (code, description) values ('outOfCompliancePrintRun', 'Out of Compliance - Print Run');
insert into permission_status (code, description) values ('outOfComplianceExpired', 'Out of Compliance - Expired');
insert into permission_status (code, description) values ('contractInsufficient', 'Granted - Insufficient');
insert into permission_status (code, description) values ('grantedInsufficientApproved', 'Granted - Insufficient Approved');
insert into permission_status (code, description) values ('grantedInsufficientPending', 'Granted Insufficient Pending');

insert into permission_status (code, description) values ('grantedPublicDomain', 'Granted - Public Domain');
insert into permission_status (code, description) values ('grantedWileyOwned', 'Granted - Wiley Owned');
insert into permission_status (code, description) values ('grantedAuthorCreated', 'Granted - Author Created/Owned');
insert into permission_status (code, description) values ('grantedFairUse', 'Granted - Fair Use');
insert into permission_status (code, description) values ('grantedRFUnlimited', 'Granted - RF Unlimited Seats & Print');
insert into permission_status (code, description) values ('grantedRoyaltyFreeLimitedPrint', 'Granted - RF Limited Print');
insert into permission_status (code, description) values ('grantedRoyaltyFreeLimitedSeats', 'Granted - RF Limited Seats');

insert into permission_status (code, description) values ('granted', 'Granted');
insert into permission_status (code, description) values ('grantedLimited', 'Granted - Limited');
-- grantedLimitedPrint is a special status that the users will not know about (description same as grantedLimited)
insert into permission_status (code, description) values ('grantedLimitedPrint', 'Granted - Limited Print Run');
insert into permission_status (code, description) values ('grantedStmGuidelines', 'Granted - STM Guidelines');
insert into permission_status (code, description) values ('grantedWorkForHire', 'Granted - Work For Hire');
insert into permission_status (code, description) values ('grantedMigratedFromAustralia', 'Migrated from Australia ePermissions');
insert into permission_status (code, description) values ('grantedMigratedFromFilemaker', 'Migrated from Filemaker');
insert into permission_status (code, description) values ('legacyUploadReviewedUnknown', 'Legacy Upload - Reviewed Unknown');
insert into permission_status (code, description) values ('legacyUploadAuthorProvided', 'Legacy Upload - Author Provided/Unknown');


insert into owner_type (code, description, sort_order) values ('Wiley Owned', 'Wiley/Author Provided', 1);
insert into owner_type (code, description, sort_order) values ('3rd Party', '3rd Party', 2);
insert into owner_type (code, description, sort_order) values ('Public Domain', 'Public Domain', 3);
insert into owner_type (code, description, sort_order) values ('Fair Use', 'Fair Use', 4);
insert into owner_type (code, description, sort_order) values ('Photo Request', 'Photo Request', 5);
insert into owner_type (code, description, sort_order) values ('Illustration Request', 'Illustration Request', 6);
insert into owner_type (code, description, sort_order) values ('Author Owned', 'Author Owned', 7);
insert into owner_type (code, description, sort_order) values ('Work For Hire', 'Work For Hire', 8);
insert into owner_type (code, description, sort_order) values ('Wiley Created', 'Wiley Created', 9);

insert into media_type (code, description, sort_order) values ('Animation', 'Animation', 1);
insert into media_type (code, description, sort_order) values ('Article', 'Article', 2);
insert into media_type (code, description, sort_order) values ('Audio', 'Audio', 3);
insert into media_type (code, description, sort_order) values ('Cartoon', 'Cartoon', 4);
insert into media_type (code, description, sort_order) values ('Clip Art', 'Clip Art', 5);
insert into media_type (code, description, sort_order) values ('CoverDesign', 'Cover Design', 6);
insert into media_type (code, description, sort_order) values ('DataSet', 'Data Set', 7);
insert into media_type (code, description, sort_order) values ('Extract', 'Extract', 8);
insert into media_type (code, description, sort_order) values ('Graph', 'Graph', 9);
insert into media_type (code, description, sort_order) values ('Icon', 'Icon', 10);
insert into media_type (code, description, sort_order) values ('Illustration', 'Illustration', 11);
insert into media_type (code, description, sort_order) values ('Interactivity', 'Interactivity', 12);
insert into media_type (code, description, sort_order) values ('LecturePresentation', 'Lecture Presentation', 13);
insert into media_type (code, description, sort_order) values ('List', 'List', 14);
insert into media_type (code, description, sort_order) values ('Map', 'Map', 15);
insert into media_type (code, description, sort_order) values ('Photo', 'Photo', 16);
insert into media_type (code, description, sort_order) values ('Question', 'Question', 17);
insert into media_type (code, description, sort_order) values ('Realia', 'Realia', 18);
insert into media_type (code, description, sort_order) values ('Screenshot', 'Screenshot', 19);
insert into media_type (code, description, sort_order) values ('Table', 'Table', 20);
insert into media_type (code, description, sort_order) values ('Text', 'Text', 21);
insert into media_type (code, description, sort_order) values ('Tutorial', 'Tutorial', 22);
insert into media_type (code, description, sort_order) values ('Video', 'Video', 23);


insert into data_type (code, description) values ('money', 'Money');
insert into data_type (code, description) values ('text', 'Text');
insert into data_type (code, description) values ('textarea', 'Textarea');
insert into data_type (code, description) values ('radio', 'Radio');
insert into data_type (code, description) values ('checkbox', 'Checkbox');

insert into data_source (code, description) values ('US', 'United States');
insert into data_source (code, description) values ('CA', 'Canada');
insert into data_source (code, description) values ('AU', 'Austraila');
insert into data_source (code, description) values ('UK', 'United Kingdom');
insert into data_source (code, description) values ('SG', 'Singapore');
insert into data_source (code, description) values ('DE', 'Germany');

insert into currency (code, description, sort_order) values ('USD', 'USD (US Dollar)', 1);
insert into currency (code, description, sort_order) values ('CAD', 'CAD (Canadian Dollar)', 2);
insert into currency (code, description, sort_order) values ('AUD', 'AUD (Australian Dollar)', 3);
insert into currency (code, description, sort_order) values ('EUR', 'EUR (Euro)', 4);
insert into currency (code, description, sort_order) values ('GBP', 'GBP (British Pound)', 5);
insert into currency (code, description, sort_order) values ('JPY', 'JPY (Japanese Yen)', 6);
insert into currency (code, description, sort_order) values ('CNY', 'CNY (Chinese Yuan Renminbi)', 7);
insert into currency (code, description, sort_order) values ('RUB', 'RUB (Russian Rouble)', 8);
insert into currency (code, description, sort_order) values ('ISPCR','ISPCR (iStockphoto credits)',10);

insert into user_location values ('US', 'US');
insert into user_location values ('CA', 'Canada');
insert into user_location values ('UK', 'UK');
insert into user_location values ('DE', 'Germany');
insert into user_location values ('AU', 'Australia');
insert into user_location values ('SG', 'Singapore');
insert into user_location values ('IN', 'India');
insert into user_location values ('CN', 'China');

insert into rendition_type (code, description, sort_order) values ('original', 'Original', 1);
insert into rendition_type (code, description, sort_order) values ('small_th', 'Small Thumbnail', 2);
insert into rendition_type (code, description, sort_order) values ('medium_th', 'Medium Thumbnail', 3);
insert into rendition_type (code, description, sort_order) values ('large_th', 'Large Thumbnail', 4);

insert into photo_estimate_type (code, description, data_type, sort_order) values ('pre-contract', 'Pre-Contract', 'money', 1);
insert into photo_estimate_type (code, description, data_type, sort_order) values ('pre-prod', 'Pre-Production', 'money', 2);
insert into photo_estimate_type (code, description, data_type, sort_order) values ('ms-to-comp', 'ms-to-comp', 'money', 3);
insert into photo_estimate_type (code, description, data_type, sort_order) values ('mid-production', 'Mid-Production', 'money', 4);
insert into photo_estimate_type (code, description, data_type, sort_order) values ('final', 'Final', 'money', 5);
insert into photo_estimate_type (code, description, data_type, sort_order) values ('repro_fee', 'Repro Fee', 'money', 6);
insert into photo_estimate_type (code, description, data_type, sort_order) values ('preliminary','Preliminary','money', 7);
insert into photo_estimate_type (code, description, data_type, sort_order) values ('actual-cost','Actual Cost','money', 8);

insert into medium (code, name) values ('P', 'Paper');
insert into medium (code, name) values ('C', 'Cloth');
insert into medium (code, name) values ('O', 'Online Products-all types');
insert into medium (code, name) values ('W', 'Website');
insert into medium (code, name) values ('CD', 'CD');
insert into medium (code, name) values ('DVD', 'DVD');
-- All Electronic Rights is fictive value that encapsulates multiple of other values
-- So DON'T add it to this table
insert into medium (code, name) values ('A', 'Audio*');
insert into medium (code, name) values ('B', 'Binders, 3- or 5-ring');
insert into medium (code, name) values ('D', 'Display stands, trays etc');
insert into medium (code, name) values ('Q', 'Content only');
insert into medium (code, name) values ('H', 'Downloadable content');
insert into medium (code, name) values ('E', 'E-books');
insert into medium (code, name) values ('F', 'Film*');
insert into medium (code, name) values ('J', 'Journal');
insert into medium (code, name) values ('K', 'Kit Nonbook/Nonelectronic');
insert into medium (code, name) values ('L', 'Loose-leaf');
insert into medium (code, name) values ('M', 'Marketing equipment (UK)');
insert into medium (code, name) values ('N', 'Newsletter');
insert into medium (code, name) values ('I', 'Slides');
insert into medium (code, name) values ('S', 'Software*');
insert into medium (code, name) values ('T', 'Transparency');
insert into medium (code, name) values ('V', 'Video*');
insert into medium (code, name) values ('X', 'Value Pack (Set) in Aus*');

insert into role_type (code, description) values ('EMPLOYEE', 'Employee');
insert into role_type (code, description) values ('AUTHOR', 'Author');

insert into role (role_type, code, is_global, description) values ('EMPLOYEE', 'SUPER', true, 'Super User');
insert into role (role_type, code, is_global, description) values ('AUTHOR', 'AUTHOR_DEFAULT', true, 'Default Author');
insert into role (role_type, code, is_global, description) values ('EMPLOYEE', 'EMPLOYEE_DEFAULT', true, 'Default Employee');
insert into role (role_type, code, is_global, description) values ('EMPLOYEE', 'PRINT_RUN_ADMIN', true, 'Print Run Admin');
insert into role (code, role_type, is_global, description) values ('ADMIN_EMAIL_RECEIVER', 'EMPLOYEE', true, 'Admin role for receiving emails');
insert into role (code, role_type, is_global, description) values('DEV', 'EMPLOYEE', true, 'Developer');
insert into role (code, role_type, is_global, description) values('ADMIN', 'EMPLOYEE', true, 'Administrator');

insert into user_2_role (user_id, role_id) select ut.id, r.id from user_table ut, role r  where ut.email='lnagy@wiley.com' and r.code='DEV';
insert into user_2_role (user_id, role_id) select ut.id, r.id from user_table ut, role r  where ut.email='nmedrano@wiley.com' and r.code='DEV';
insert into user_2_role (user_id, role_id) select ut.id, r.id from user_table ut, role r  where ut.email='smarkoff@wiley.com' and r.code='DEV';
insert into user_2_role (user_id, role_id) select ut.id, r.id from user_table ut, role r  where ut.email='lnagy@wiley.com' and r.code='ADMIN';
insert into user_2_role (user_id, role_id) select ut.id, r.id from user_table ut, role r  where ut.email='nmedrano@wiley.com' and r.code='ADMIN';
insert into user_2_role (user_id, role_id) select ut.id, r.id from user_table ut, role r  where ut.email='smarkoff@wiley.com' and r.code='ADMIN';
insert into user_2_role (user_id, role_id) select ut.id, r.id from user_table ut, role r  where ut.email='jhopkin@wiley.com' and r.code='ADMIN';

insert into privilege (code, description, is_global, author_ok, sort_order) values ('create_po', 'Create Purchase Orders', false, true, 3);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('copy_assets', 'Copy assets from another edition', false, true, 4);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('copy_assets_product', 'Copy assets from another product', false, true, 5);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('cover_assets', 'View/Enter cover assets', false, true, 6);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('enable_request', 'Enable photo research requests', false, true, 7);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('upload_asset_file', 'Upload asset file', false, true, 9);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('manage_components', 'Manage components', false, true, 10);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('edit_chapters', 'Edit/Enter assets for all chapters', false, true, 11);
insert into privilege (code, description, is_global, author_ok, sort_order) values ('in_production_active', 'Edit ''In Production'' products', false, true, 12);

insert into user_type (code, description) values ('EMPLOYEE', 'Employee');
insert into user_type (code, description) values ('AUTHOR', 'Author');
insert into user_type (code, description) values ('FREELANCER', 'Freelancer');
insert into user_type (code, description) values ('SYSTEM', 'System');

-- Need to include created_date and last_updated_date since MySQL doesn't allow current_timestamp as a default
insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, code, password_encrypted)
values (current_timestamp, current_timestamp, 'SYSTEM', 'Permissions', 'System', 'permissionsSystem@wiley.com', 'permissions', 'fddcdd922c8db55d90bc2482c4b88df648563ce9');

insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, code, password_encrypted)
values (current_timestamp, current_timestamp, 'SYSTEM', 'Product Engineering', 'Client', 'pe@foo.com', 'peclient', '');

insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, code, password_encrypted)
values (current_timestamp, current_timestamp, 'SYSTEM', 'CMS', 'Client', 'cms@foo.com', 'cmsclient', '');

insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, code, password_encrypted)
values (current_timestamp, current_timestamp, 'SYSTEM', 'WEB', 'Client', 'web@foo.com', 'webclient', '');

insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, code, password_encrypted)
values (current_timestamp, current_timestamp, 'SYSTEM', 'Demo', 'User', 'demo@wiley.com', 'demouser', 'fddcdd922c8db55d90bc2482c4b88df648563ce9');

insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, ldap_dn, code,  password_encrypted)
values (current_timestamp, current_timestamp, 'EMPLOYEE', 'Steve', 'Markoff', 'smarkoff@wiley.com',
'cn=Steve Markoff, ou=San Francisco, ou=United States, ou=North America, ou=Wiley Users, dc=wiley, dc=com', 'smarkoff', null);

insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, ldap_dn, code,  password_encrypted)
values (current_timestamp, current_timestamp, 'EMPLOYEE', 'Napoleon', 'Medrano', 'nmedrano@wiley.com',
'cn=Napoleon Medrano, ou=San Francisco, ou=United States, ou=North America, ou=Wiley Users, dc=wiley, dc=com', 'nmedrano',  null);

insert into user_table (created_date, last_updated_date, user_type, first_name, last_name, email, ldap_dn, code,  password_encrypted)
values (current_timestamp, current_timestamp, 'EMPLOYEE', 'Luminita', 'nagy', 'lnagy@wiley.com',
'cn=Luminita Nagy, ou=San Francisco, ou=United States, ou=North America, ou=Wiley Users, dc=wiley, dc=com', 'lnagy',  null);

insert into wiley_entity values ('WILEY_US', 'John Wiley & Sons, Inc.');
insert into wiley_entity values ('WILEY_UK', 'John Wiley & Sons UK LLP');
insert into wiley_entity values ('WILEY_CA', 'John Wiley & Sons Canada Ltd.');
insert into wiley_entity values ('WILEY_ASIA', 'John Wiley & Sons Singapore Pte. Ltd.');
insert into wiley_entity values ('WILEY_AUSTRALIA', 'John Wiley & Sons Australia, Ltd.');
insert into wiley_entity values ('WILEY_SUB', 'Wiley Subscription Services, Inc.');
insert into wiley_entity values ('WILEY_LTD', 'John Wiley & Sons, Ltd.');
insert into wiley_entity values ('WILEY_LLC', 'Wiley Publishing LLC');
insert into wiley_entity values ('BKL_PUB', 'Blackwell Publishing Ltd.');
insert into wiley_entity values ('BKL_PUB_ASIA', 'Blackwell Publishing Asia Pty Ltd.');
insert into wiley_entity values ('WILEY_PUB_AU', 'Wiley Publishing Australia Pty Ltd.');
insert into wiley_entity values ('WRIGHT_PTY', 'Wrightbooks Pty Ltd.');
insert into wiley_entity values ('WILEY_VCH', 'Wiley VCH Verlag GMBH & Co KgaA');
insert into wiley_entity values ('WILEY_INDIA', 'Wiley India Private');
insert into wiley_entity values ('BKL_KK', 'Blackwell Publishing KK');

insert into user_defaults (user_id, return_address, user_signature)
values ((select id from user_table where email = 'smarkoff@wiley.com'), 'Steve''s Return Address', 'Steve''s User Signature');


insert into user_defaults (user_id, return_address, user_signature)
values ((select id from user_table where email = 'nmedrano@wiley.com'), 'Napoleon''s Return Address', 'Napoleon''s User Signature');

insert into user_2_role (user_id, role_id) select id, (select id from role where code = 'SUPER') from user_table where code in ('smarkoff', 'nmedrano', 'lnagy');
insert into user_2_role (user_id, role_id) select id, (select id from role where code = 'ADMIN_EMAIL_RECEIVER') from user_table where code in ('smarkoff');
insert into user_2_role (user_id, role_id) select id, (select id from role where code = 'EMPLOYEE_DEFAULT') from user_table where user_type = 'SYSTEM' or user_type = 'EMPLOYEE';
insert into user_2_role (user_id, role_id) select id, (select id from role where code = 'AUTHOR_DEFAULT') from user_table where user_type = 'AUTHOR';

insert into usage_type (code, description, abbreviation, sort_order) values ('Back Cover', 'Back Cover', 'back cov', 1);
insert into usage_type (code, description, abbreviation, sort_order) values ('Case Art', 'Case Art', 'case art', 2);
insert into usage_type (code, description, abbreviation, sort_order) values ('CDROM', 'CDROM', 'CDROM', 3);
insert into usage_type (code, description, abbreviation, sort_order) values ('Endpapers', 'Endpapers', 'endppr', 4);
insert into usage_type (code, description, abbreviation, sort_order) values ('Exhibit', 'Exhibit', 'exhibit', 5);
insert into usage_type (code, description, abbreviation, sort_order) values ('Feature', 'Feature', 'feature', 6);
insert into usage_type (code, description, abbreviation, sort_order) values ('Figure', 'Figure', 'fig', 7);
insert into usage_type (code, description, abbreviation, sort_order) values ('Flaps', 'Flaps', 'flap', 8);
insert into usage_type (code, description, abbreviation, sort_order) values ('Front Cover', 'Front Cover', 'frnt cov', 9);
insert into usage_type (code, description, abbreviation, sort_order) values ('Front Matter', 'Front Matter', 'frnt mat', 10);
insert into usage_type (code, description, abbreviation, sort_order) values ('Icon', 'Icon', 'icon', 11);
insert into usage_type (code, description, abbreviation, sort_order) values ('Inline Text', 'Inline Text', 'inline', 12);
insert into usage_type (code, description, abbreviation, sort_order) values ('Opener', 'Opener', 'opener', 13);
insert into usage_type (code, description, abbreviation, sort_order) values ('Spine', 'Spine', 'spine', 14);
insert into usage_type (code, description, abbreviation, sort_order) values ('Table', 'Table', 'table', 15);
insert into usage_type (code, description, abbreviation, sort_order) values ('Unnumbered Figure', 'Unnumbered Figure', 'unnum', 16);
insert into usage_type (code, description, abbreviation, sort_order) values ('Wrap Cover', 'Wrap Cover', 'wrap cov', 17);
insert into usage_type (code, description, abbreviation, sort_order) values ('Box Feature', 'Box Feature', 'box feat', 18);

-- Should not need these anymore -- if these values exist in any import, map "Cover" to "Front Cover"
-- and "Numbered Figure" to "Figure" rather than adding these values back into the database
--insert into usage_type (code, description, sort_order) values ('Cover', 'Cover', 17);
--insert into usage_type (code, description, sort_order) values ('Numbered Figure', 'Numbered Figure', 18);

insert into size_enum (code, description, sort_order) values('1/8 Page', '1/8 Page', 1);
insert into size_enum (code, description, sort_order) values ('1/4 Page', '1/4 Page', 2);
insert into size_enum (code, description, sort_order) values ('1/3 Page', '1/3 Page', 3);
insert into size_enum (code, description, sort_order) values ('1/2 Page', '1/2 Page', 4);
insert into size_enum (code, description, sort_order) values ('3/4 Page', '3/4 Page', 5);
insert into size_enum (code, description, sort_order) values ('Full Page','Full Page', 6);
insert into size_enum (code, description, sort_order) values ('1 1/4 Page', '1 1/4 Page', 7);
insert into size_enum (code, description, sort_order) values ('1 1/2 Page','1 1/2 Page', 8);
insert into size_enum (code, description, sort_order) values ('1 3/4 Page', '1 3/4 Page', 9);
insert into size_enum (code, description, sort_order) values ('Double Page', 'Double Page', 10);
insert into size_enum (code, description, sort_order) values ('Spot', 'Spot', 11);
insert into size_enum (code, description, sort_order) values ('N/A', 'N/A', 12);

-- Note we also have app. logic to insert business units after a product load is done
-- from PE, but it may be good to have these pre-populated so that we can lookup
-- the business unit name from the code on the search (before the user clicks
-- on the product).
-- I would think that the "Unassigned" codes should never be used but we have
-- received some product notification messages that specified businessUnit="9".
insert into business_unit (code, name) values ('0', 'Unassigned');
insert into business_unit (code, name) values ('1', 'Higher Education');
insert into business_unit (code, name) values ('2', 'Professional and Trade');
insert into business_unit (code, name) values ('3', 'Sci/Tech/Med/Scholarly');
insert into business_unit (code, name) values ('4', 'Miscellaneous');
insert into business_unit (code, name) values ('5', 'Unassigned');
insert into business_unit (code, name) values ('6', 'Unassigned');
insert into business_unit (code, name) values ('7', 'Unassigned');
insert into business_unit (code, name) values ('8', 'Unassigned');
insert into business_unit (code, name) values ('9', 'Unassigned');

update business_unit set email = 'jhopkin@wiley.com';

-- Make this acurate later
--insert into product_line (code, name) values ('D', '??');

-- Add more for this later
--insert into product_edition (external_id, name, edition_number, product_line)
--values ('1', 'Gray MIS Edition 1', 10, 'D');

insert into submedium (code, data_source, name) values ('D3', 'US', '*3-1/2" Disk');
insert into submedium (code, data_source, name) values ('D5', 'US', '*5-1/4" Disk');
insert into submedium (code, data_source, name) values ('DM', 'US', '*Dual(3-1/2&5-1/4")Disks');
insert into submedium (code, data_source, name) values ('CD', 'US', 'CD');
insert into submedium (code, data_source, name) values ('MC', 'US', '*Macintosh Disk');
insert into submedium (code, data_source, name) values ('MM', 'US', 'Multimedia Package');
insert into submedium (code, data_source, name) values ('VD', 'US', 'Videodisk');
insert into submedium (code, data_source, name) values ('VV', 'US', '*VHS Videotape');
insert into submedium (code, data_source, name) values ('SP', 'US', 'Spiral-bound');
insert into submedium (code, data_source, name) values ('FS', 'US', '*Filmstrip');
insert into submedium (code, data_source, name) values ('AC', 'US', 'Audiotape Cassette');
insert into submedium (code, data_source, name) values ('RR', 'US', '*Reel-to-Reel Audio Tape');
insert into submedium (code, data_source, name) values ('CN', 'US', 'CD-ROM w/network license');
insert into submedium (code, data_source, name) values ('OK', 'US', 'Online Key');
insert into submedium (code, data_source, name) values ('WI', 'US', 'Interscience');
insert into submedium (code, data_source, name) values ('OT', 'US', '3rd-party sourced content');
insert into submedium (code, data_source, name) values ('AS', 'US', 'AS/400-sourced content');
insert into submedium (code, data_source, name) values ('LS', 'US', 'Late Subscription');
insert into submedium (code, data_source, name) values ('IN', 'US', 'CD-ROM w/intranet license');
insert into submedium (code, data_source, name) values ('LL', 'US', 'Loose-leaf');
insert into submedium (code, data_source, name) values ('DV', 'US', 'DVD disk');
insert into submedium (code, data_source, name) values ('NL', 'US', '*Net Library');
insert into submedium (code, data_source, name) values ('PP', 'US', '*Peanut Press');
insert into submedium (code, data_source, name) values ('WB', 'US', '*Web Buy');
insert into submedium (code, data_source, name) values ('MR', 'US', '*Microsoft Reader');
insert into submedium (code, data_source, name) values ('RB', 'US', '*Rocket Books');
insert into submedium (code, data_source, name) values ('TA', 'US', 'Tabs (3 ring binder)');
insert into submedium (code, data_source, name) values ('WS', 'US', 'WebSite Associated w/Book');
insert into submedium (code, data_source, name) values ('CO', 'US', 'CD-ROM + On-line');
insert into submedium (code, data_source, name) values ('OL', 'US', 'Online Data');
insert into submedium (code, data_source, name) values ('MA', 'US', 'Mass Market Paperback');
insert into submedium (code, data_source, name) values ('RL', 'US', '*Film Reel');
insert into submedium (code, data_source, name) values ('LF', 'US', 'Lay-flat binding');
insert into submedium (code, data_source, name) values ('IB', 'US', 'Interscience Book online');
insert into submedium (code, data_source, name) values ('IR', 'US', 'Intersci Reference online');
insert into submedium (code, data_source, name) values ('IP', 'US', 'Intersci. Protocol online');
insert into submedium (code, data_source, name) values ('ID', 'US', 'Intersci. Database online');
insert into submedium (code, data_source, name) values ('BF', 'US', 'Intersci Backfiles online');
insert into submedium (code, data_source, name) values ('OA', 'US', 'Online Application');
insert into submedium (code, data_source, name) values ('OC', 'US', 'Online Course');
insert into submedium (code, data_source, name) values ('PS', 'US', 'Prepaid set');
insert into submedium (code, data_source, name) values ('DW', 'US', 'DVD + Website accompany');
insert into submedium (code, data_source, name) values ('LB', 'US', 'Log Book');
insert into submedium (code, data_source, name) values ('UB', 'US', 'Unbound Book Block');
-- smarkoff: I assume the data_source for these *(UK) submediums is UK but not sure
-- check production later or ask Bruce or request submedium master list (not working right now) 
insert into submedium (code, data_source, name) values ('EK', 'UK', '*(UK) Exhibition Kit');
insert into submedium (code, data_source, name) values ('MS', 'UK', '*(UK) Memory Stick');
insert into submedium (code, data_source, name) values ('CB', 'UK', '*(UK) Cloth Bag');
insert into submedium (code, data_source, name) values ('SW', 'UK', '*(UK) Sweets');
insert into submedium (code, data_source, name) values ('MU', 'UK', '*(UK) Mugs');
insert into submedium (code, data_source, name) values ('WT', 'UK', '*(UK) Water Bottles');
insert into submedium (code, data_source, name) values ('CL', 'UK', '*(UK) Clothing');
insert into submedium (code, data_source, name) values ('KY', 'UK', '*(UK) Keyring');
insert into submedium (code, data_source, name) values ('TG', 'UK', '*(UK) Travel Giveaways');
insert into submedium (code, data_source, name) values ('UM', 'UK', '*(UK) Umbrella');
insert into submedium (code, data_source, name) values ('HT', 'UK', '*(UK) Hats');
insert into submedium (code, data_source, name) values ('RP', 'UK', '*(UK) Reprint Booklet');
----
insert into submedium (code, data_source, name) values ('CA', 'US', 'Card for Online Access');
insert into submedium (code, data_source, name) values ('LP', 'US', 'Large Print');
insert into submedium (code, data_source, name) values ('GL', 'US', '*PDF e-book');
insert into submedium (code, data_source, name) values ('MB', 'US', '*Mobipocket e-book');
insert into submedium (code, data_source, name) values ('ML', 'US', '*Mobi-lite e-book');
insert into submedium (code, data_source, name) values ('EP', 'US', '*ePub e-book');

-- These are for message types. Corresponds to MessageType enum in Message
insert into msg_type (code, description) values ('REQUEST', 'A Request');
insert into msg_type (code, description) values ('REPLY', 'A Reply');
insert into msg_type (code, description) values ('NOTIFICATION', 'A Notification');
insert into msg_type (code, description) values ('FAILURE', 'A Failure');
insert into msg_type (code, description) values ('ERROR', 'An Error');

-- These are for message operation types. Corresponds to OperationType enum in Message
insert into msg_operation_type (code, description) values ('ERROR', 'Error');
insert into msg_operation_type (code, description) values ('UPDATE_ASSET', 'UpdateAsset');
insert into msg_operation_type (code, description) values ('DELETE_ASSET', 'DeleteAsset');
insert into msg_operation_type (code, description) values ('GET_ASSET', 'GetAsset');
insert into msg_operation_type (code, description) values ('UPDATE_ASSET_USE', 'UpdateAssetUse');
insert into msg_operation_type (code, description) values ('DELETE_ASSET_USE', 'DeleteAssetUse');
insert into msg_operation_type (code, description) values ('GET_ASSET_USE', 'GetAssetUse');
insert into msg_operation_type (code, description) values ('UPDATE_SOURCE', 'UpdateSource');
insert into msg_operation_type (code, description) values ('GET_SOURCE', 'GetSource');
insert into msg_operation_type (code, description) values ('DELETE_SOURCE', 'DeleteSource');
insert into msg_operation_type (code, description) values ('UPDATE_COMPONENT', 'UpdateComponent');
insert into msg_operation_type (code, description) values ('GET_COMPONENT', 'GetComponent');
insert into msg_operation_type (code, description) values ('DELETE_COMPONENT', 'DeleteComponent');
insert into msg_operation_type (code, description) values ('GET_COMPONENT_LIST', 'GetComponentList');
insert into msg_operation_type (code, description) values ('REPORT_COMPONENT_LIST', 'ReportComponentList');
insert into msg_operation_type (code, description) values ('PRODUCT_UPDATE', 'productUpdate');
insert into msg_operation_type (code, description) values ('MASTER_LIST_UPDATE', 'masterListUpdate');
insert into msg_operation_type (code, description) values ('ALL_NEW_PRODUCTS', 'allNewProducts');
insert into msg_operation_type (code, description) values ('ASSET_USE_COLLECTION_STATUS_VALIDATION', 'AssetUseCollectionStatusValidation');
insert into msg_operation_type (code, description) values ('ASSET_COLLECTION_STATUS_VALIDATION', 'AssetCollectionStatusValidation');
insert into msg_operation_type (code, description) values ('CW_STATUS_VALIDATION', 'CwStatusValidation');
insert into msg_operation_type (code, description) values ('SEARCH_PRODUCTS', 'SearchProducts');
insert into msg_operation_type (code, description) values ('GET_LATEST_PRODUCT', 'GetLatestProduct');
insert into msg_operation_type (code, description) values ('UPDATE_PRODUCTS', 'UpdateProducts');
insert into msg_operation_type (code, description) values ('UPDATE_MASTER_LISTS', 'UpdateMasterLists');
insert into msg_operation_type (code, description) values ('UPDATE_PRODUCT', 'UpdateProduct');
insert into msg_operation_type (code, description) values ('CREATE_SOURCE', 'CreateSource');

-- Message statuses.  Corresponds to MessageStatus enum in Message
insert into msg_status (code, description) values ('NEW', 'New');
insert into msg_status (code, description) values ('PROCESSING', 'A Message Being Processed');
insert into msg_status (code, description) values ('PENDING_MORE_INFORMATION', 'A Message Waiting On More Information');
insert into msg_status (code, description) values ('PROCESSED', 'A Processed Message');
insert into msg_status (code, description) values ('FAILED', 'A Failure');
insert into msg_status (code, description) values ('TERMINALLY_FAILED', 'A Failure that has failed several retries.');

-- Delivery Methods
insert into delivery_method (code, description, sort_order) values ('mail', 'Post/Mail', 1);
insert into delivery_method (code, description, sort_order) values ('email', 'E-Mail', 2);
insert into delivery_method (code, description, sort_order) values ('fax', 'Fax', 3);

-- Accounts
insert into account (acct_number, sub_code, description) values ('6113', '4200', 'COPYRIGHTS, PERMISS - Corp vendor');
insert into account (acct_number, sub_code, description) values ('6113', '8001', 'COPYRIGHTS, PERMISS - Inhse contract');
insert into account (acct_number, sub_code, description) values ('6113', '9900', 'COPYRIGHTS, PERMISS - Individual vendor');
insert into account (acct_number, sub_code, description) values ('1341', '2001', 'ROYALTIES');
insert into account (acct_number, sub_code, description) values ('1309', '2001', 'COMP PERMISSIONS-ADDS');

-- Component Categories
insert into component_category (code, description) values ('AA', 'About the Author');
insert into component_category (code, description) values ('AC', 'Acknowledgments');
insert into component_category (code, description) values ('ACW', 'Abbreviated Concept');
insert into component_category (code, description) values ('ADW', 'Audio');
insert into component_category (code, description) values ('AN', 'Author''s Note');
insert into component_category (code, description) values ('ANW', 'Animations');
insert into component_category (code, description) values ('AP', 'Appendix');
insert into component_category (code, description) values ('AR', 'MRW Article');
insert into component_category (code, description) values ('ASW', 'Assignment');
insert into component_category (code, description) values ('BAW', 'Back Matter');
insert into component_category (code, description) values ('BC', 'Bonus Chapter');
-- CMS may have this but James want to force all cover components to have same CVW category
--insert into component_category (code, description) values ('BE', 'Inside Back Cover');
insert into component_category (code, description) values ('BG', 'A Biographical Article in an MRW');
insert into component_category (code, description) values ('BI', 'Bibliography');
insert into component_category (code, description) values ('BM', 'General Backmatter');
insert into component_category (code, description) values ('BOW', 'Book Companion Site');
insert into component_category (code, description) values ('BUW', 'Business Plan');
insert into component_category (code, description) values ('CA', 'Current Protocols Appendix Unit');
insert into component_category (code, description) values ('CH', 'Chapter');
insert into component_category (code, description) values ('CI', 'Color Insert');
insert into component_category (code, description) values ('CON', 'Contract');
insert into component_category (code, description) values ('COW', 'Concept Module');
insert into component_category (code, description) values ('CP', 'Current Protocols Ordinary Unit');
insert into component_category (code, description) values ('CR', 'Chronology');
insert into component_category (code, description) values ('CS', 'Case Study');
insert into component_category (code, description) values ('CVW', 'Cover');
insert into component_category (code, description) values ('DAW', 'Data Sets');
insert into component_category (code, description) values ('DC', 'Dedication');
insert into component_category (code, description) values ('DEW', 'Design');
insert into component_category (code, description) values ('DM', 'Digital Media');
insert into component_category (code, description) values ('DF', 'A Definition Entry in an MRW');
insert into component_category (code, description) values ('DJ', 'Dust Jacket Copy');
insert into component_category (code, description) values ('DR', 'Diagnostic Review');
insert into component_category (code, description) values ('EB', 'Editorial Board');
insert into component_category (code, description) values ('EG', 'Epigraph');
insert into component_category (code, description) values ('EN', 'Endorsements');
insert into component_category (code, description) values ('ENW', 'End matter');
insert into component_category (code, description) values ('EP', 'Epilogue');
insert into component_category (code, description) values ('ER', 'Erratum');
insert into component_category (code, description) values ('EXW', 'Example');
-- CMS may have this but James want to force all cover components to have same CVW category
--insert into component_category (code, description) values ('FE', 'Inside Front Cover');
insert into component_category (code, description) values ('FIW', 'Financials');
insert into component_category (code, description) values ('FRW', 'Front Matter');
insert into component_category (code, description) values ('FW', 'Foreword');
insert into component_category (code, description) values ('GL', 'Glossary');
insert into component_category (code, description) values ('GRW', 'Graphics');
insert into component_category (code, description) values ('HI', 'Historical Perspective');
insert into component_category (code, description) values ('HIW', 'Higher Practice');
insert into component_category (code, description) values ('ICW', 'Icons');
insert into component_category (code, description) values ('ILW', 'Illustrations');
insert into component_category (code, description) values ('IMW', 'Image');
insert into component_category (code, description) values ('IN', 'Index');
insert into component_category (code, description) values ('IND', 'Interior Design');
insert into component_category (code, description) values ('INW', 'Interactives');
insert into component_category (code, description) values ('IR', 'Intervention Review');
insert into component_category (code, description) values ('IT', 'Introduction');
insert into component_category (code, description) values ('KN', 'Keynote (in ELS)');
insert into component_category (code, description) values ('LAW', 'Layout');
insert into component_category (code, description) values ('LI', 'Lists of Tables, Figures, Maps, etc.');
insert into component_category (code, description) values ('ME', 'Media Items, Including Disclaimers');
insert into component_category (code, description) values ('MEW', 'Media Spec');
insert into component_category (code, description) values ('MR', 'Methodology Review');
insert into component_category (code, description) values ('OV', 'Overview');
insert into component_category (code, description) values ('PE', 'Part Epilogue');
insert into component_category (code, description) values ('PF', 'Book or Series Preface');
insert into component_category (code, description) values ('PHW', 'Photos');
insert into component_category (code, description) values ('PI', 'Part Introduction');
insert into component_category (code, description) values ('PN', 'Publisher''s Note');
insert into component_category (code, description) values ('PP', 'Generic Front matter');
insert into component_category (code, description) values ('PR', 'Prologue');
insert into component_category (code, description) values ('PRF', 'Preface');
insert into component_category (code, description) values ('PRM', 'Promotion');
insert into component_category (code, description) values ('PRW', 'Practice');
insert into component_category (code, description) values ('PV', 'Provisional Record');
insert into component_category (code, description) values ('REW', 'Review');
insert into component_category (code, description) values ('RFR', 'References');
insert into component_category (code, description) values ('SA', 'Structured Abstract');
insert into component_category (code, description) values ('SBC', 'Sub concept');
insert into component_category (code, description) values ('SBI', 'Subject Index');
insert into component_category (code, description) values ('SH', 'Short Article in an MRW');
insert into component_category (code, description) values ('SI', 'Subpart Introduction');
insert into component_category (code, description) values ('SPW', 'Specifications');
insert into component_category (code, description) values ('SS', 'Sub subpart Introduction');
insert into component_category (code, description) values ('SUW', 'Survey');
insert into component_category (code, description) values ('TN', 'Translator''s Note');
insert into component_category (code, description) values ('UR', 'Overview of Reviews');
insert into component_category (code, description) values ('VIW', 'Videos');
insert into component_category (code, description) values ('WEW', 'Web Resources');
insert into component_category (code, description) values ('XX', 'Miscellaneous');

insert into page_pos_enum values ('top', 'Top', 1);
insert into page_pos_enum values ('bottom', 'Bottom', 2);
insert into page_pos_enum values ('top right', 'Top Right', 3);
insert into page_pos_enum values ('top left', 'Top Left', 4);
insert into page_pos_enum values ('bottom right', 'Bottom Right', 5);
insert into page_pos_enum values ('bottom left', 'Bottom Left', 6);
insert into page_pos_enum values ('left', 'Left', 7);
insert into page_pos_enum values ('right', 'Right', 8);
insert into page_pos_enum values ('center', 'Center', 9);
insert into page_pos_enum values ('center left', 'Center Left', 10);
insert into page_pos_enum values ('center right', 'Center Right', 11);
insert into page_pos_enum values ('top center', 'Top Center', 12);
insert into page_pos_enum values ('bottom center', 'Bottom Center', 13);
insert into page_pos_enum(code,description,sort_order) values ('N/A','N/A',0);

-- creates a dummy source used by the wiley-owned + managed logic
insert into source (id, external_id, external_name, credit_line, delivery_method, disabled)
  values(1, 'perm.source.1', 'John Wiley & Sons', '', null, 0);
-- This is MySQL-specific syntax
alter table source AUTO_INCREMENT = 2;

insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'componentName', 'Component', true, true, true, true, true, 10, 1);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'manuscriptPage', 'MS Page', true, false, true, true, true, 7, 2);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'mediaType', 'Media Type', true, true, true, true, true, 7, 3);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'description', 'Description', true, true, true, true, true, 15, 4);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'usage', 'Usage', true, true, true, true, true, 7, 5);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'position', 'Position', true, true, true, true, true, 7, 6);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'sourcesAsHTMLLinks', 'Sources', true, true, true, true, false, 15, 7);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'permissionStatus', 'Status', true, true, true, true, false, 10, 8);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'finalPage', 'Final Page', false, false, true, true, true, 5, 9);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'sourceRefNumber', 'Source Ref. Number', false, false, true, true, true, 7, 10);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'creditLine', 'Credit Line', false, false, true, true, true, 10, 11);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'pickup', 'Pickup', false, false, true, true, true, 4, 12);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order) 
	values (1, 'royaltyFree', 'Royalty Free', false, false, true, true, false, 4, 13);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order) 
	values (1, 'cameraCopyToCome', 'Camera Copy To Come', false, false, true, true, true, 4, 14);
insert into user_profile(user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order) 
	values (1, 'sentToProduction', 'Sent To Production', false, false, true, true, true, 4, 15);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'permissionComment', 'Comments', false, false, false, true, true, 15, 16);
insert into user_profile (user_id, name, title, visible, required, sortable, searchable, editable, width, sort_order)
	values (1, 'sortOrder', 'Asset Order', false, false, true, true, true, 15, 17);
insert into user_profile (user_id, name, title, visible, required, sortable,searchable,editable, width, sort_order)
	values(1,'gbpmCategory','GBPM Category', false, false, true, true, true, 15, 18);

update user_profile set sort_type = 'natural' where name='position';
update user_profile set sort_type='title-numeric' where name='componentName';

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('medium', 'What media types are granted?', null, null, 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('medium_all', 'All media types including future types', 'medium', 'radio', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('medium_physical_ebook_web', 'All physical media and ebook/web', 'medium', 'radio', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('medium_physical_electronic', 'All physical and electronic media', 'medium', 'radio', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('medium_all_physical', 'All physical media including print and CDROM', 'medium', 'radio', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('medium_print_only', 'Print only', 'medium', 'radio', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('medium_no_mention', 'No mention of media types', 'medium', 'radio', 6);


-- These are ISO 3166-2 country codes. See http://en.wikipedia.org/wiki/ISO_3166-2
-- Except uk is used by the European Commission instead of gb and we use that here
-- (no other country has uk).

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales', 'What distribution or sales territory rights are granted?', null, null, 20);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_world_alias', 'Worldwide', 'sales', 'radio', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_north_america_alias', 'North America', 'sales', 'radio', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_world', 'Other', 'sales', 'radio', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_no_mention', 'No mention of distribution/sales territories', 'sales', 'radio', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_north_america', 'North America', 'sales_world', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_us', 'United States', 'sales_north_america', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ca', 'Canada', 'sales_north_america', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mx', 'Mexico', 'sales_north_america', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_europe', 'Europe', 'sales_world', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ad', 'Andorra', 'sales_europe', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_at', 'Austria', 'sales_europe', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_be', 'Belgium', 'sales_europe', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_fr', 'France', 'sales_europe', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_de', 'Germany', 'sales_europe', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gr', 'Greece', 'sales_europe', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_it', 'Italy', 'sales_europe', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_li', 'Liechtenstein', 'sales_europe', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_lu', 'Luxembourgh', 'sales_europe', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mt', 'Malta', 'sales_europe', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mc', 'Monaco', 'sales_europe', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_nl', 'Netherlands', 'sales_europe', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_pt', 'Portugal', 'sales_europe', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ie', 'Republic of Ireland', 'sales_europe', 'checkbox', 14);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_es', 'Spain', 'sales_europe', 'checkbox', 15);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sm', 'San Marino', 'sales_europe', 'checkbox', 16);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ch', 'Switzerland', 'sales_europe', 'checkbox', 17);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_uk', 'United Kingdom', 'sales_europe', 'checkbox', 18);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_uk-e', 'England', 'sales_uk', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_uk-ni', 'Northern Ireland', 'sales_uk', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_uk-s', 'Scotland', 'sales_uk', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_uk-w', 'Wales', 'sales_uk', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_nordic', 'Nordic', 'sales_europe', 'checkbox', 19);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_fi', 'Finland', 'sales_nordic', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_is', 'Iceland', 'sales_nordic', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_no', 'Norway', 'sales_nordic', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_se', 'Sweden', 'sales_nordic', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_eastern_europe', 'Eastern Europe', 'sales_world', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_al', 'Albania', 'sales_eastern_europe', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_am', 'Armenia', 'sales_eastern_europe', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_az', 'Azerbaijan', 'sales_eastern_europe', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_by', 'Belarus', 'sales_eastern_europe', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ba', 'Bosnia and Herzegovina', 'sales_eastern_europe', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bg', 'Bulgaria', 'sales_eastern_europe', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_hr', 'Croatia', 'sales_eastern_europe', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cz', 'Czech Republic', 'sales_eastern_europe', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ee', 'Estonia', 'sales_eastern_europe', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ge', 'Georgia', 'sales_eastern_europe', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_hu', 'Hungary', 'sales_eastern_europe', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_kz', 'Kazakhstan', 'sales_eastern_europe', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_xk', 'Kosovo', 'sales_eastern_europe', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_lv', 'Latvia', 'sales_eastern_europe', 'checkbox', 14);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_lt', 'Lithuania', 'sales_eastern_europe', 'checkbox', 15);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mk', 'Macedonia', 'sales_eastern_europe', 'checkbox', 16);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_md', 'Moldova', 'sales_eastern_europe', 'checkbox', 17);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_me', 'Montenegro', 'sales_eastern_europe', 'checkbox', 18);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_pl', 'Poland', 'sales_eastern_europe', 'checkbox', 19);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ro', 'Romania', 'sales_eastern_europe', 'checkbox', 20);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ru', 'Russia', 'sales_eastern_europe', 'checkbox', 21);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_rs', 'Serbia', 'sales_eastern_europe', 'checkbox', 22);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_si', 'Slovenia', 'sales_eastern_europe', 'checkbox', 23);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sk', 'Slovakia', 'sales_eastern_europe', 'checkbox', 24);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ua', 'Ukraine', 'sales_eastern_europe', 'checkbox', 25);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_asia', 'Asia', 'sales_world', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bd', 'Bangladesh', 'sales_asia', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bt', 'Bhutan', 'sales_asia', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bn', 'Brunei', 'sales_asia', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_kh', 'Cambodia', 'sales_asia', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cn', 'China', 'sales_asia', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_in', 'India', 'sales_asia', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_id', 'Indonesia', 'sales_asia', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_jp', 'Japan', 'sales_asia', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_la', 'Laos', 'sales_asia', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_my', 'Malaysia', 'sales_asia', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mv', 'Maldives', 'sales_asia', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mn', 'Mongolia', 'sales_asia', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mm', 'Myanmar', 'sales_asia', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_np', 'Nepali', 'sales_asia', 'checkbox', 14);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_kp', 'North Korea', 'sales_asia', 'checkbox', 15);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_pk', 'Pakistan', 'sales_asia', 'checkbox', 16);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_pg', 'Papua New Guinea', 'sales_asia', 'checkbox', 17);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ph', 'Philippines', 'sales_asia', 'checkbox', 18);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sg', 'Singapore', 'sales_asia', 'checkbox', 19);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_kr', 'South Korea', 'sales_asia', 'checkbox', 20);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_lk', 'Sri Lanka', 'sales_asia', 'checkbox', 21);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_tw', 'Taiwan', 'sales_asia', 'checkbox', 22);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_th', 'Thailand', 'sales_asia', 'checkbox', 23);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_vn', 'Vietnam', 'sales_asia', 'checkbox', 24);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mid_east', 'Middle East', 'sales_world', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_af', 'Afghanistan', 'sales_mid_east', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_dz', 'Algeria', 'sales_mid_east', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bh', 'Bahrain', 'sales_mid_east', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cy', 'Cyprus', 'sales_mid_east', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_eg', 'Egypt', 'sales_mid_east', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ir', 'Iran', 'sales_mid_east', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_iq', 'Iraq', 'sales_mid_east', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_il', 'Israel', 'sales_mid_east', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_jo', 'Jordan', 'sales_mid_east', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ku', 'Kuwait', 'sales_mid_east', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_lb', 'Lebanon', 'sales_mid_east', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ly', 'Libya', 'sales_mid_east', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_om', 'Oman', 'sales_mid_east', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_qa', 'Qatar', 'sales_mid_east', 'checkbox', 14);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sa', 'Saudi Arabia', 'sales_mid_east', 'checkbox', 15);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sd', 'Sudan', 'sales_mid_east', 'checkbox', 16);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sy', 'Syria', 'sales_mid_east', 'checkbox', 17);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_tn', 'Tunisia', 'sales_mid_east', 'checkbox', 18);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_tr', 'Turkey', 'sales_mid_east', 'checkbox', 19);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ae', 'United Arab Emirates', 'sales_mid_east', 'checkbox', 20);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ye', 'Yemen', 'sales_mid_east', 'checkbox', 21);

insert into condition_type (code, description, parent_code, data_type,sort_order)
values ('sales_africa', 'Africa', 'sales_world', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ao', 'Angola', 'sales_africa', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bj', 'Benin', 'sales_africa', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bw', 'Botswana', 'sales_africa', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bf', 'Burkina Faso', 'sales_africa', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bi', 'Burundi', 'sales_africa', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cm', 'Cameroon', 'sales_africa', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cv', 'Cape Verde', 'sales_africa', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cf', 'Central African Republic', 'sales_africa', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_td', 'Chad', 'sales_africa', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cg', 'Congo', 'sales_africa', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cd', 'Democratic Republic of the Congo', 'sales_africa', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_dj', 'Djibouti', 'sales_africa', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gq', 'Equatorial Guinea', 'sales_africa', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_er', 'Eritrea', 'sales_africa', 'checkbox', 14);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_et', 'Ethiopia', 'sales_africa', 'checkbox', 15);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ga', 'Gabon', 'sales_africa', 'checkbox', 16);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gm', 'Gambia', 'sales_africa', 'checkbox', 17);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gh', 'Ghana', 'sales_africa', 'checkbox', 18);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gw', 'Guinea-Bissau', 'sales_africa', 'checkbox', 19);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gn', 'Guinea', 'sales_africa', 'checkbox', 20);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ci', 'Ivory Coast', 'sales_africa', 'checkbox', 21);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ke', 'Kenya', 'sales_africa', 'checkbox', 22);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ls', 'Lesotho', 'sales_africa', 'checkbox', 23);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_lr', 'Liberia', 'sales_africa', 'checkbox', 24);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mg', 'Madagascar', 'sales_africa', 'checkbox', 25);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mw', 'Malawi', 'sales_africa', 'checkbox', 26);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ml', 'Mali', 'sales_africa', 'checkbox', 27);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mr', 'Mauritania', 'sales_africa', 'checkbox', 28);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mu', 'Mauritius', 'sales_africa', 'checkbox', 29);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ma', 'Morocoo', 'sales_africa', 'checkbox', 30);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_mz', 'Mozambique', 'sales_africa', 'checkbox', 31);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_na', 'Nambia', 'sales_africa', 'checkbox', 32);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ne', 'Niger', 'sales_africa', 'checkbox', 33);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ng', 'Nigeria', 'sales_africa', 'checkbox', 34);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_re', 'R�union', 'sales_africa', 'checkbox', 35);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_rw', 'Rwanda', 'sales_africa', 'checkbox', 36);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_st', 'S�o Tom� and Pr�ncipe', 'sales_africa', 'checkbox', 37);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sn', 'Senegal', 'sales_africa', 'checkbox', 38);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sc', 'Seychelles', 'sales_africa', 'checkbox', 39);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sl', 'Sierra Leone', 'sales_africa', 'checkbox', 40);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_so', 'Somalia', 'sales_africa', 'checkbox', 41);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sz', 'South Africa', 'sales_africa', 'checkbox', 42);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_za', 'Swaziland', 'sales_africa', 'checkbox', 43);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_tz', 'Tanzania', 'sales_africa', 'checkbox', 44);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_tg', 'Togo', 'sales_africa', 'checkbox', 45);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ug', 'Uganda', 'sales_africa', 'checkbox', 46);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_zm', 'Zambia', 'sales_africa', 'checkbox', 47);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_zw', 'Zimbabwe', 'sales_africa', 'checkbox', 48);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_australia', 'Australia', 'sales_world', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_au', 'Australia', 'sales_australia', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_nz', 'New Zealand', 'sales_australia', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_central_am', 'Central America', 'sales_world', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bz', 'Belize', 'sales_central_am', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cr', 'Costa Rica', 'sales_central_am', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sv', 'El Salvador', 'sales_central_am', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gt', 'Guatemala', 'sales_central_am', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_hn', 'Honduras', 'sales_central_am', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ni', 'Nicaragua', 'sales_central_am', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_pa', 'Panama', 'sales_central_am', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_south_america', 'South America', 'sales_world', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ar', 'Argentina', 'sales_south_america', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_bo', 'Bolivia', 'sales_south_america', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_br', 'Brazil', 'sales_south_america', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_cl', 'Chile', 'sales_south_america', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_co', 'Columbia', 'sales_south_america', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ec', 'Ecuador', 'sales_south_america', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_fk', 'Falkland Islands (UK)', 'sales_south_america', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gf', 'French Guiana (France)', 'sales_south_america', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gy', 'Guyana', 'sales_south_america', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_py', 'Paraguay', 'sales_south_america', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_pe', 'Peru', 'sales_south_america', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_gs', 'South Georgia and South Sandwich Islands (UK)', 'sales_south_america', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_sr', 'Suriname', 'sales_south_america', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_uy', 'Uruguay', 'sales_south_america', 'checkbox', 14);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sales_ve', 'Venezuela', 'sales_south_america', 'checkbox', 15);


-- These are ISO 639-2 language codes. See http://www.loc.gov/standards/iso639-2/php/code_list.php
-- 639-3 is newer and has one difference in these languages - French is "fra" instead of "fre".
-- But PE uses "fre" for French.

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language', 'What language rights are granted?', null, null, 30);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_all_alias', 'All Languages', 'language', 'radio', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_eng_alias', 'English only', 'language', 'radio', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_all', 'Other (select as many as apply)', 'language', 'radio', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_no_mention', 'No mention of language', 'language', 'radio', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_eng', 'English', 'language_all', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_a_c', 'Other languages: A-C', 'language_all', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_d_h', 'Other languages: D-H', 'language_all', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_i_l', 'Other languages: I-L', 'language_all', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_m_r', 'Other languages: M-R', 'language_all', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_s', 'Other languages: S', 'language_all', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_t_z', 'Other languages: T-Z', 'language_all', 'checkbox', 7);



insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_alb', 'Albanian', 'language_a_c', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_amh', 'Amharic', 'language_a_c', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ara', 'Arabic', 'language_a_c', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_arm', 'Armenian', 'language_a_c', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_aze', 'Azerbaijani', 'language_a_c', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ben', 'Bengali', 'language_a_c', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_bel', 'Belarusian', 'language_a_c', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_bos', 'Bosnian', 'language_a_c', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_bul', 'Bulgarian', 'language_a_c', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_mya', 'Burmese', 'language_a_c', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_cat', 'Catalan', 'language_a_c', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_nya', 'Chewa', 'language_a_c', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_zho', 'Chinese', 'language_a_c', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_hrv', 'Croatian', 'language_a_c', 'checkbox', 14);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_cze', 'Czech', 'language_a_c', 'checkbox', 15);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_div', 'Dhivehi', 'language_d_h', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_dut', 'Dutch', 'language_d_h', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_dzo', 'Dzongkha', 'language_d_h', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_est', 'Estonian', 'language_d_h', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_fil', 'Filipino', 'language_d_h', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_fin', 'Finnish', 'language_d_h', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_fre', 'French', 'language_d_h', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_geo', 'Georgian', 'language_d_h', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_deu', 'German', 'language_d_h', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_gre', 'Greek', 'language_d_h', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_heb', 'Hebrew', 'language_d_h', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_hin', 'Hindi', 'language_d_h', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_hun', 'Hungarian', 'language_d_h', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ice', 'Icelandic', 'language_i_l', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ind', 'Indonesian', 'language_i_l', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ita', 'Italian', 'language_i_l', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_jpn', 'Japanese', 'language_i_l', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_kaz', 'Kazakhstan', 'language_i_l', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_khm', 'Khmer', 'language_i_l', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_run', 'Kirundi', 'language_i_l', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_kor', 'Korean', 'language_i_l', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_kur', 'Kurdish', 'language_i_l', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_lao', 'Lao', 'language_i_l', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_lav', 'Latvian', 'language_i_l', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_lit', 'Lithuanian', 'language_i_l', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ltz', 'Luxembourgish', 'language_i_l', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_mac', 'Macedonian', 'language_m_r', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_mlg', 'Malagasy', 'language_m_r', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_msa', 'Malay', 'language_m_r', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_mlt', 'Maltese', 'language_m_r', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_mol', 'Moldovan', 'language_m_r', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_mon', 'Mongolian', 'language_m_r', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_nep', 'Nepali', 'language_m_r', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_nor', 'Norwegian', 'language_m_r', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_pol', 'Polish', 'language_m_r', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_per', 'Persian', 'language_m_r', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_por', 'Portugese', 'language_m_r', 'checkbox', 11);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ron', 'Romanian', 'language_m_r', 'checkbox', 12);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_rus', 'Russian', 'language_m_r', 'checkbox', 13);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_srp', 'Serbian', 'language_s', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_sha', 'Shona', 'language_s', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_slv', 'Slovene', 'language_s', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_slo', 'Slovak', 'language_s', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_som', 'Somali', 'language_s', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_sot', 'Sotho', 'language_s', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_spa', 'Spanish', 'language_s', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_swa', 'Swahili', 'language_s', 'checkbox', 8);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ssw', 'Swazi', 'language_s', 'checkbox', 9);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_swe', 'Swedish', 'language_s', 'checkbox', 10);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_nam', 'Taiwanese', 'language_t_z', 'checkbox', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_tha', 'Thai', 'language_t_z', 'checkbox', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_tir', 'Tigrinya', 'language_t_z', 'checkbox', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_tur', 'Turkish', 'language_t_z', 'checkbox', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_ukr', 'Ukrainian', 'language_t_z', 'checkbox', 5);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_urd', 'Urdu', 'language_t_z', 'checkbox', 6);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_vie', 'Vietnamese', 'language_t_z', 'checkbox', 7);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('language_cym', 'Welsh', 'language_t_z', 'checkbox', 8);


insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('print_run_top', 'Does the grant/letter include any reference to print run?', null, null, 40);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('print_run_unlimited', 'Unlimited print run is granted', 'print_run_top', 'radio', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('print_run_limit', 'Maximum copies Wiley can print', 'print_run_top', 'radio', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('print_run_limit_box', '', 'print_run_limit', 'text', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('print_run_no_mention', 'Print run is not mentioned', 'print_run_top', 'radio', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('print_run_ebook', 'Total Ebook Print Run', 'print_run', 'text', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('edition', 'Does the grant/letter specify use in particular editions?', null, null, 50);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('edition_c_f_author', 'Granted for this, future editions and/or entire author series', 'edition', 'radio', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('edition_all_c_and_f', 'Granted for this edition and all future editions', 'edition', 'radio', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('edition_this', 'Granted for this edition only', 'edition', 'radio', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('edition_no_mention', 'No mention of editions', 'edition', 'radio', 4);


insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('dwork', 'Does the grant cover use of the asset(s) in derivative works?', null, null, 60);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('dwork_all', 'Wiley can include the asset(s) in any ancillaries, derivatives and custom works', 'dwork', 'radio', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('dwork_anc_and_deriv', 'Wiley can include the asset(s) in ancillaries and derivatives with the exception of custom', 'dwork', 'radio', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('dwork_main_only', 'Grant states asset(s) cannot be included in any derivative works', 'dwork', 'radio', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('dwork_no_mention', 'No mention of derivative works', 'dwork', 'radio', 4);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sublicense', 'Are sublicensing rights granted?', null, null, 95);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sublicense_right', 'Wiley can include the asset(s) when sub-licensing product', 'sublicense', 'radio', 1);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sublicense_no_right', 'Wiley cannot include the asset(s) when sub-licensing product', 'sublicense', 'radio', 2);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('sublicense_no_mention', 'The grant does not mention sublicensing', 'sublicense', 'radio', 3);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('other', 'Other restrictions and permission notes', null, null, 100);

insert into condition_type (code, description, parent_code, data_type, sort_order)
values ('other_notes', 'Notes', 'other', 'textarea', 1);

update condition_type set can_see_in_cw = true, can_edit_in_cw = true
  where code = 'print_run' or code like 'language%';
update condition_type set can_see_in_cw = false, can_edit_in_cw = false
  where code in ('language_no_mention', 'print_run_no_mention', 'print_run_ebook');

update condition_type set can_edit_in_ct = false, can_edit_in_ct = false
  where code = 'print_run_ebook';

update condition_type set can_see_in_madeal = true, can_edit_in_madeal = true
  where code = 'print_run' or code like 'language%' or code like 'edition%' or code like 'usage%' or code like 'sublicense%';

update condition_type set can_see_in_madeal = false, can_edit_in_madeal = false
  where code = 'print_run_ebook' or code like '%_no_mention';

update condition_type set validation_class='com.wiley.permissions.services.datatypes.NumericType' where code='print_run';


insert into copyright_type values ('0','NOT Wiley owned');
insert into copyright_type values ('1','Wiley owned - Work for hire');
insert into copyright_type values ('2','Wiley owned - Copyright Transfer Agreement');

insert into enum_cancel_reason values ('1', 'Entry error');
insert into enum_cancel_reason values ('2', 'Spec dropped');
insert into enum_cancel_reason values ('3', 'Budgetary issue');
insert into enum_cancel_reason values ('4', 'Spec changed');
insert into enum_cancel_reason values ('5', 'Selection changed');
insert into enum_cancel_reason values ('6', 'Source Perm denial');
insert into enum_cancel_reason values ('7', 'Source is on NoFly list');

insert into user_group (id, name, description) values (1,'GE Photo Editors', 'GE Photo Editors');
insert into user_group (id, name, description) values (2,'Creative Services', 'Creative Services US PD');
insert into user_group (id, name, description) values (2,'PD Editorial', 'PD Editorial');
insert into user_group (id, name, description) values (6,'GE Editorial', 'GE Editorial');
insert into user_group (id, name, description) values (7,'GR Editorial', 'GR Editorial');
insert into user_group (id, name, description) values (9,'Corporate', 'Corporate');
insert into user_group (id, name, description) values (9,'Authors', 'Authors');
insert into user_group (id, name, description) values (10,'Freelancers', 'Freelancers');
insert into user_group (id, name, description) values (11,'AUS Editorial', 'Australia Editorial');

insert into enum_usage_condition(code, description, sort_order) values ('Front Cover', 'Front Cover', 1);
insert into enum_usage_condition(code, description, sort_order) values ('Back Cover', 'Back Cover', 2);
insert into enum_usage_condition(code, description, sort_order) values ('Spine', 'Spine', 3);
insert into enum_usage_condition(code, description, sort_order) values ('Wrap Cover', 'Wrap Cover', 4);
insert into enum_usage_condition(code, description, sort_order) values ('Flaps', 'Flaps', 5);
insert into enum_usage_condition(code, description, sort_order) values ('Interior(single use)', 'Interior (single use)', 6);
insert into enum_usage_condition(code, description, sort_order) values ('Interior(multi use)', 'Interior (multi use)', 7);
insert into enum_usage_condition(code, description, sort_order) values ('Opener/Epigraph', 'Opener/Epigraph', 8);
insert into enum_usage_condition(code, description, sort_order) values ('Marketing/Promo', 'Marketing/Promo', 9);
insert into enum_usage_condition(code, description, sort_order) values ('CDROM', 'CDROM', 10);

insert into enum_language values ('en', 'English');
insert into enum_language values ('de', 'German');
insert into enum_language values ('es', 'Spanish');

insert into gbpm_category (code, description) values (908, '908-Royalty Free Image');
insert into gbpm_category (code, description) values (909, '909-Rights Managed Image');
insert into gbpm_category (code, description) values (910, '910-Text Articles');
insert into gbpm_category (code, description) values (911, '911-Text Extracts');
insert into gbpm_category (code, description) values (912, '912-Permissionable figure/map');
insert into gbpm_category (code, description) values (913, '913-ACARA');
insert into gbpm_category (code, description) values (914, '914-NSWBOS');
insert into gbpm_category (code, description) values (915, '915-VCAA');
insert into gbpm_category (code, description) values (916, '916-Permissionable cartoons');
insert into gbpm_category (code, description) values (920, '920-Screen dumps');
insert into gbpm_category (code, description) values (117, '117-Royalty Free Image');
insert into gbpm_category (code, description) values (118, '118-Rights Managed Image');
insert into gbpm_category (code, description) values (119, '119-Text Articles');
insert into gbpm_category (code, description) values (120, '120-Text Extracts');
insert into gbpm_category (code, description) values (122, '122-Permissionable figure/map');

insert into import_source(code, description) values(1, 'GE Filemaker');
insert into import_source(code, description) values(2, 'CS SpreadSheet');
insert into import_source(code, description) values(3, 'Australia');
insert into import_source(code, description) values(4, 'Rights Link');
insert into import_source(code, description) values(5, 'Legacy Upload');

