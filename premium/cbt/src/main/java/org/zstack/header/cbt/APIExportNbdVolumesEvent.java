package org.zstack.header.cbt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = "all")
public class APIExportNbdVolumesEvent extends APIEvent {
    private List<VolumeCbtBackupInfo> volumeInfos;

    public APIExportNbdVolumesEvent() {
    }

    public APIExportNbdVolumesEvent(String msgId) {
        super(msgId);
    }

    public List<VolumeCbtBackupInfo> getVolumeInfos() {
        return volumeInfos;
    }

    public void setVolumeInfos(List<VolumeCbtBackupInfo> volumeInfos) {
        this.volumeInfos = volumeInfos;
    }
}
