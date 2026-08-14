package org.zstack.storage.device.localRaid;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

public class SelfTestLocalRaidForRecordMsg extends NeedReplyMessage {
    private List<String> uuids = new ArrayList<>();

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuid) {
        this.uuids = uuid;
    }
}
