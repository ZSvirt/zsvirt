package org.zstack.header.vm;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by weiwang on 04/09/2017
 */
public class GetVmStartingCandidateClustersHostsMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

}