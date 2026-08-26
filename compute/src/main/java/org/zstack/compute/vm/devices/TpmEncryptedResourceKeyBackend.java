package org.zstack.compute.vm.devices;

import org.zstack.header.core.Completion;

/**
 * Responsible for handling the replication or reset of encryption resource keys
 * and other tasks in VM TPM cloning scenarios.
 */
public interface TpmEncryptedResourceKeyBackend {

    /**
     * Build relationship from {@link org.zstack.header.tpm.entity.TpmVO} to EncryptedResourceKeyRefVO
     * Non-async call.
     */
    void attachKeyProviderToTpm(String tpmUuid, String keyProviderUuid);

    /**
     * Clean relationship from {@link org.zstack.header.tpm.entity.TpmVO} to EncryptedResourceKeyRefVO
     * Non-async call.
     */
    void detachKeyProviderFromTpm(String tpmUuid);

    /**
     * maybe null (when crypto module is not installed)
     */
    String findKeyProviderUuidByTpm(String tpmUuid);

    /**
     * maybe null (when crypto module is not installed)
     */
    String findKeyProviderUuidByName(String providerName);

    /**
     * maybe null (when crypto module is not installed)
     */
    String findKeyProviderNameByTpm(String tpmUuid);

    /**
     * maybe null (when crypto module is not installed)
     */
    Integer findKeyVersionByTpm(String tpmUuid);

    /**
     * maybe null (when crypto module is not installed)
     */
    String defaultKeyProviderUuid();

    /**
     * Repair providerUuid on existing TPM key ref row where wrapped material already exists.
     * @return updated rows
     */
    int applyKeyProviderWithKek(String tpmUuid, String providerUuid);

    /**
     * Whether an EncryptedResourceKeyRef row exists for this TPM.
     */
    boolean checkTpmKeyProviderAttached(String tpmUuid);

    static class CloneEncryptedResourceKeyContext {
        public String srcTpmUuid;
        public String dstTpmUuid;

        /**
         * Whether to reset (regenerate) the key on the target TPM.
         * <ul>
         *   <li>{@code true}：Regenerate the key for the target TPM
         *   without inheriting the encrypted data from the source TPM.</li>
         *   <li>{@code false}：Copy the existing keys from the source TPM
         *   to the target TPM to ensure they remain consistent.</li>
         * </ul>
         */
        public boolean resetTpm;
    }

    /**
     * In a VM cloning scenario, copy or reset the encryption resource key
     * from the source TPM to the target TPM.
     */
    void cloneEncryptedResourceKey(CloneEncryptedResourceKeyContext context, Completion completion);

    static class BackupEncryptedResourceKeyContext {
        public String srcResourceUuid;
        public String dstResourceUuid;
    }

    /**
     * Copy the encryption resource key from the source TPM to {@link org.zstack.header.vm.additions.VmHostBackupFileVO}
     * or other resources.
     */
    void backupEncryptedResourceKey(BackupEncryptedResourceKeyContext context);

    static class RestoreEncryptedResourceKeyContext {
        public String srcResourceUuid;
        public String dstResourceUuid;
    }

    void restoreEncryptedResourceKey(RestoreEncryptedResourceKeyContext context);

    void cleanEncryptedResourceKey(String vmHostBackupFileUuid);

    /**
     * Remove encryption key material stored for a {@link org.zstack.header.tpm.entity.TpmKeyBackupVO}.
     */
    void cleanTpmKeyBackupEncryptedResourceKey(String tpmKeyBackupUuid);
}
