package org.zstack.routeProtocol;

import org.zstack.header.message.Message;

public class DisableProtocolInAreaMsg extends Message {
    private String areaUuid;

    public String getAreaUuid() {
        return areaUuid;
    }

    public void setAreaUuid(String areaUuid) {
        this.areaUuid = areaUuid;
    }
}
