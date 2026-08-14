package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author : jingwang
 * @create 2023/4/20 6:15 PM
 */
public class GetHostPowerStatusMsg extends NeedReplyMessage implements HostMessage  {
    private String uuid;

    private HostPowerManagementMethod method;

    public HostPowerManagementMethod getMethod() {
        return method;
    }

    public void setMethod(HostPowerManagementMethod method) {
        this.method = method;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
