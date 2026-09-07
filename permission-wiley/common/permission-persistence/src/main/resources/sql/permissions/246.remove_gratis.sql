-- merged with create and seed

alter table asset drop column is_gratis;

delete from permission_status where code = 'grantedCourtesyGratis';

-- just to cleanup data for local machines -- don't run in production
--update asset set is_managed = 1 where id in (select asset_id from asset_use where permission_status = 'unpaid');
--update asset_use set permission_status = 'granted' where permission_status = 'unpaid';
--update au_source_perm_status set permission_status = 'granted' where permission_status = 'unpaid';
--update asset_perm_ref set permission_status = 'granted' where permission_status = 'unpaid';

delete from permission_status where code = 'unpaid';
delete from permission_status where code = 'denied';
delete from permission_status where code = 'grantedRFLimited';

update permission_status set description = 'Not Requested - No Source' where code = 'noSource';
update permission_status set description = 'Not Requested - Will be Work for Hire' where code = 'notRequestedBeWorkForHire';
update permission_status set description = 'Not Requested - Will be Royalty Free' where code = 'notRequestedBeRoyaltyFree';
update permission_status set description = 'Granted - Insufficient' where code = 'contractInsufficient';
update permission_status set description = 'Granted - Insufficient Approved' where code = 'grantedInsufficientApproved';
update permission_status set description = 'Granted - Insufficient Pending' where code = 'grantedInsufficientPending';
update permission_status set description = 'Out of Compliance - Print Run' where code = 'outOfCompliancePrintRun';
update permission_status set description = 'Out of Compliance - Expired' where code = 'outOfComplianceExpired';
update permission_status set description = 'Granted - Public Domain' where code = 'grantedPublicDomain';
update permission_status set description = 'Granted - Wiley Owned' where code = 'grantedWileyOwned';
update permission_status set description = 'Granted - Author Created/Owned' where code = 'grantedAuthorCreated';
update permission_status set description = 'Granted - Fair Use' where code = 'grantedFairUse';
update permission_status set description = 'Granted - RF Unlimited Seats & Print' where code = 'grantedRFUnlimited';
update permission_status set description = 'Granted - RF Limited Print' where code = 'grantedRoyaltyFreeLimitedPrint';
update permission_status set description = 'Granted - RF Limited Seats' where code = 'grantedRoyaltyFreeLimitedSeats';
update permission_status set description = 'Granted - Limited' where code = 'grantedLimited';
update permission_status set description = 'Granted - Limited' where code = 'grantedLimitedPrint';
update permission_status set description = 'Granted - Work For Hire' where code = 'grantedWorkForHire';
update permission_status set description = 'Granted - STM Guidelines' where code = 'grantedStmGuidelines';
update permission_status set description = 'Legacy Upload - Reviewed Unknown' where code = 'legacyUploadReviewedUnknown';
update permission_status set description = 'Legacy Upload - Author Provided/Unknown' where code = 'legacyUploadAuthorProvided';
update permission_status set description = 'Migrated from Australia ePermissions' where code = 'grantedMigratedFromAustralia';
update permission_status set description = 'Migrated from Filemaker' where code = 'grantedMigratedFromFilemaker';

-- now recalculate all statuses (drop trigger history_au_u  and add back after)
