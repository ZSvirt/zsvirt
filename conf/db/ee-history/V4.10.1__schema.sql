CALL INSERT_COLUMN('L2NetworkHostRefVO', 'skipDeletion', 'boolean', 0, false, 'bridgeName');

delete from `EncryptEntityMetadataVO` where `entityName` in ('AppBuildSystemVO', 'IAM2VirtualIDAttributeVO');
