package org.zstack.header.vipQos;


import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by weiwang on 2/08/2017.
 */
public class SyncVipQosMsg extends NeedReplyMessage {

    private String vipUuid;
    private String vrUuid;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVrUuid() {
        return vrUuid;
    }

    public void setVrUuid(String vrUuid) {
        this.vrUuid = vrUuid;
    }
}
