package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author : jingwang
 * @create 2023/4/21 4:42 PM
 */
public class RebootHostMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private boolean returnEarly;
    private HostPowerManagementMethod method = HostPowerManagementMethod.AUTO;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isReturnEarly() {
        return returnEarly;
    }

    public void setReturnEarly(boolean returnEarly) {
        this.returnEarly = returnEarly;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public HostPowerManagementMethod getMethod() {
        return method;
    }

    public void setMethod(HostPowerManagementMethod method) {
        this.method = method;
    }
}
