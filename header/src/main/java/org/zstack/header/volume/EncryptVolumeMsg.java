package org.zstack.header.volume;

import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

/**
 * Convert an existing data volume's bits to a LUKS-encrypted form in place.
 * <p>
 * Steps performed by the handler (in {@link org.zstack.storage.volume.VolumeBase}):
 * <ol>
 *   <li>Ensure the volume has a key-provider binding (auto-attaches the default key provider
 *       when none is bound yet).</li>
 *   <li>Materialize a DEK via the {@code EncryptedResourceKeyManager}.</li>
 *   <li>Seal the DEK for the target host as {@code encryptedDek}; the kvmagent
 *       creates any single-use secret material file locally as needed.</li>
 *   <li>Ask the primary storage backend to LUKS-convert the bits in place
 *       ({@code EncryptVolumeBitsOnPrimaryStorageMsg}).</li>
 *   <li>Persist {@code VolumeVO.encrypted = true}.</li>
 * </ol>
 * <p>
 * If the volume row is already marked {@code encrypted=true}, the handler treats it as a
 * no-op success. The {@code encrypted} flag is the single authoritative signal that
 * "the bits on disk are already LUKS"; callers must NOT pre-mark the row before invoking
 * this message.
 */
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 1)
public class EncryptVolumeMsg extends NeedReplyMessage implements VolumeMessage, ConfigurableTimeoutMessage {
    private String volumeUuid;
    private String hostUuid;
    /**
     * Optional. When null, the handler resolves it from {@code VolumeVO.primaryStorageUuid}.
     */
    private String primaryStorageUuid;
    /**
     * Optional. When null, the handler resolves it from {@code VolumeVO.installPath}.
     */
    private String installPath;
    /**
     * Free-form purpose label for the DEK get-or-create audit trail.
     */
    private String purpose;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
