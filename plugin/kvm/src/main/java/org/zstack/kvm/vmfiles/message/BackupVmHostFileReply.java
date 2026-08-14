package org.zstack.kvm.vmfiles.message;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class BackupVmHostFileReply extends MessageReply {
    private List<String> backupFileUuidList;

    public List<String> getBackupFileUuidList() {
        return backupFileUuidList;
    }

    public void setBackupFileUuidList(List<String> backupFileUuidList) {
        this.backupFileUuidList = backupFileUuidList;
    }
}
