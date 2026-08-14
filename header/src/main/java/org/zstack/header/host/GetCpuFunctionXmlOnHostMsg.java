package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by LiangHanYu on 2021/7/29 15:27
 */
public class GetCpuFunctionXmlOnHostMsg extends NeedReplyMessage implements HostMessage{
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
