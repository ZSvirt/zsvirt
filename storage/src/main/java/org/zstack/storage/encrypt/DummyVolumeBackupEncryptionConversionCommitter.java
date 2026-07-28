package org.zstack.storage.encrypt;

import org.zstack.header.volume.VolumeBackupEncryptionConversionCommitter;
import org.zstack.header.volume.VolumeBackupEncryptionConversionResult;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * OSS / no-premium-crypto: no-op backup encryption-conversion committer, same role as
 * {@link DummyVolumeEncryptedResourceKeyBackend}. The real committer lives in premium crypto.xml;
 * VolumeBase skips backup conversion when this committer reports unavailable.
 */
public class DummyVolumeBackupEncryptionConversionCommitter implements VolumeBackupEncryptionConversionCommitter {
    private static final CLogger logger = Utils.getLogger(DummyVolumeBackupEncryptionConversionCommitter.class);

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void commitVolumeBackupEncryptionConversion(VolumeBackupEncryptionConversionResult result) {
        logger.debug("ignore commit volume backup encryption conversion: no premium crypto committer registered");
    }
}
