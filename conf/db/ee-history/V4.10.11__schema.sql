UPDATE `VmHaVO`
        SET `inhibitionReason` = 'for templated', `inhibitionTime` = NOW()
        WHERE `inhibitionReason` IS NULL AND `uuid` IN (SELECT `uuid` FROM `TemplatedVmInstanceVO`);
UPDATE `VmHaVO`
        SET `inhibitionReason` = 'for templated', `inhibitionTime` = NOW()
        WHERE `inhibitionReason` IS NULL AND `uuid` IN (SELECT `cacheVmInstanceUuid` FROM `TemplatedVmInstanceCacheVO`);
