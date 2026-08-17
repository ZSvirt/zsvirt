package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Triggers an in-place LUKS encryption of an existing volume file on primary storage.
 * Used after downloading a data-volume template's plain bits to LocalStorage when the
 * volume is marked encrypted: the agent converts the plain qcow2/raw at {@link #installPath}
 * into a LUKS-encrypted qcow2 (overwriting in place).
 *
 * The DEK is staged on the host out-of-band (caller stages the secret material file via
 * SecretHostEnsureLuksSecretFileMsg and passes the file path here).
 */
public class EncryptVolumeBitsOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String hostUuid;
    private String volumeUuid;
    private String installPath;
    private String encryptLuksSecretMaterialFilePath;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getEncryptLuksSecretMaterialFilePath() {
        return encryptLuksSecretMaterialFilePath;
    }

    public void setEncryptLuksSecretMaterialFilePath(String encryptLuksSecretMaterialFilePath) {
        this.encryptLuksSecretMaterialFilePath = encryptLuksSecretMaterialFilePath;
    }
}
