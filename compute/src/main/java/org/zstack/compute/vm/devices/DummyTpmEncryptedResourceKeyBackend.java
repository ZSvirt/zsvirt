package org.zstack.compute.vm.devices;

import org.zstack.header.core.Completion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class DummyTpmEncryptedResourceKeyBackend implements TpmEncryptedResourceKeyBackend {
    private static final CLogger logger = Utils.getLogger(DummyTpmEncryptedResourceKeyBackend.class);

    @Override
    public void attachKeyProviderToTpm(String tpmUuid, String keyProviderUuid) {
        logger.debug("ignore attach key provider to TPM request for TPM uuid " + tpmUuid +
                " and key provider uuid " + keyProviderUuid);
    }

    @Override
    public void detachKeyProviderFromTpm(String tpmUuid) {
        logger.debug("ignore detach key provider from TPM request for TPM uuid " + tpmUuid);
    }

    @Override
    public String findKeyProviderUuidByTpm(String tpmUuid) {
        return null;
    }

    @Override
    public String findKeyProviderUuidByName(String providerName) {
        return null;
    }

    @Override
    public String findKeyProviderNameByTpm(String tpmUuid) {
        return null;
    }

    @Override
    public Integer findKeyVersionByTpm(String tpmUuid) {
        return null;
    }

    @Override
    public String defaultKeyProviderUuid() {
        return null;
    }

    @Override
    public int applyKeyProviderWithKek(String tpmUuid, String providerUuid) {
        return 0;
    }

    @Override
    public boolean checkTpmKeyProviderAttached(String tpmUuid) {
        return false;
    }

    @Override
    public void cloneEncryptedResourceKey(CloneEncryptedResourceKeyContext context, Completion completion) {
        // do nothing
        logger.debug("ignore clone encrypted resource key request for TPM uuid "
                + context.srcTpmUuid + " -> " + context.dstTpmUuid);
        completion.success();
    }

    @Override
    public void backupEncryptedResourceKey(BackupEncryptedResourceKeyContext context) {
        // do nothing
        logger.debug("ignore backup encrypted resource key request for src resource: "
                + context.srcResourceUuid + " -> dest resource: " + context.dstResourceUuid);
    }

    @Override
    public void restoreEncryptedResourceKey(RestoreEncryptedResourceKeyContext context) {
        // do nothing
        logger.debug("ignore restore encrypted resource key request for src resource: "
                + context.srcResourceUuid + " -> dest resource: " + context.dstResourceUuid);
    }

    @Override
    public void cleanEncryptedResourceKey(String vmHostBackupFileUuid) {
        // do nothing
        logger.debug("ignore cleanup encrypted resource key request for VmHostBackupFileVO: " + vmHostBackupFileUuid);
    }
}
