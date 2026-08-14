package org.zstack.header.tpm.entity;

import org.zstack.header.rest.APINoSee;
import org.zstack.utils.StringDSL;

public class TpmSpec {
    private boolean enable = true;
    private String tpmUuid;
    private String keyProviderUuid;
    @APINoSee
    private String secretUuid;
    @APINoSee
    private String backupFileUuid;
    @APINoSee
    private boolean resourceKeyCreatedNew;
    @APINoSee
    private String resourceKeyProviderUuid;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getKeyProviderUuid() {
        return keyProviderUuid;
    }

    public void setKeyProviderUuid(String keyProviderUuid) {
        this.keyProviderUuid = keyProviderUuid;
    }

    public String getBackupFileUuid() {
        return backupFileUuid;
    }

    public void setBackupFileUuid(String backupFileUuid) {
        this.backupFileUuid = backupFileUuid;
    }

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }

    public boolean isResourceKeyCreatedNew() {
        return resourceKeyCreatedNew;
    }

    public void setResourceKeyCreatedNew(boolean resourceKeyCreatedNew) {
        this.resourceKeyCreatedNew = resourceKeyCreatedNew;
    }

    public String getResourceKeyProviderUuid() {
        return resourceKeyProviderUuid;
    }

    public void setResourceKeyProviderUuid(String resourceKeyProviderUuid) {
        this.resourceKeyProviderUuid = resourceKeyProviderUuid;
    }

    public static TpmSpec __example__() {
        TpmSpec tpm = new TpmSpec();
        tpm.setKeyProviderUuid(StringDSL.createFixedUuid("keyProviderUuid"));
        return tpm;
    }
}
