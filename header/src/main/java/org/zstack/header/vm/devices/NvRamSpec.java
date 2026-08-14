package org.zstack.header.vm.devices;

import org.zstack.header.rest.APINoSee;

public class NvRamSpec {
    private boolean needRegister;
    @APINoSee
    private String backupFileUuid;

    public boolean isNeedRegister() {
        return needRegister;
    }

    public void setNeedRegister(boolean needRegister) {
        this.needRegister = needRegister;
    }

    public String getBackupFileUuid() {
        return backupFileUuid;
    }

    public void setBackupFileUuid(String backupFileUuid) {
        this.backupFileUuid = backupFileUuid;
    }
}
