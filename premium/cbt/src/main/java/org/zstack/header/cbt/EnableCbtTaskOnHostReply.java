package org.zstack.header.cbt;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class EnableCbtTaskOnHostReply extends MessageReply {
    private List<VolumeCbtBackupInfo> volumeCbtBackupInfos;

    public List<VolumeCbtBackupInfo> getVolumeCbtBackupInfos() {
        return volumeCbtBackupInfos;
    }

    public void setVolumeCbtBackupInfos(List<VolumeCbtBackupInfo> volumeCbtBackupInfos) {
        this.volumeCbtBackupInfos = volumeCbtBackupInfos;
    }
}