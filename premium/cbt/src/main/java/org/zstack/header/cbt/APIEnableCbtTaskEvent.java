package org.zstack.header.cbt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(fieldsTo = "all")
public class APIEnableCbtTaskEvent extends APIEvent {
    private List<VolumeCbtBackupInfo> volumeCbtBackupInfos;

    public List<VolumeCbtBackupInfo> getVolumeCbtBackupInfos() {
        return volumeCbtBackupInfos;
    }

    public void setVolumeCbtBackupInfos(List<VolumeCbtBackupInfo> volumeCbtBackupInfos) {
        this.volumeCbtBackupInfos = volumeCbtBackupInfos;
    }

    public APIEnableCbtTaskEvent() {
    }

    public APIEnableCbtTaskEvent(String msgId) {
        super(msgId);
    }

    public static APIEnableCbtTaskEvent __example__() {
        APIEnableCbtTaskEvent event = new APIEnableCbtTaskEvent();
        event.setVolumeCbtBackupInfos(asList(new VolumeCbtBackupInfo()));
        return event;
    }
}
