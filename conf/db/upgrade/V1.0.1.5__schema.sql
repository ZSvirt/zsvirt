-- ZSV-13141: update existing ZMigrate VM types
UPDATE `zstack`.`VmInstanceEO` vm
INNER JOIN `zstack`.`SystemTagVO` st
        ON st.`resourceUuid` = vm.`uuid`
       AND st.`resourceType` = 'VmInstanceVO'
       AND st.`tag` IN (
           'ZMigrateManagementNodeVm',
           'ZMigrateGatewayVm'
       )
SET vm.`type` = 'MevocoVm'
WHERE vm.`deleted` IS NULL
  AND vm.`type` <> 'MevocoVm';
