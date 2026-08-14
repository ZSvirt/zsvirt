package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by LiangHanYu on 2021/8/13 16:31
 */
public class CompareCpuFunctionOnHostMsg extends NeedReplyMessage implements HostMessage{
    private String srcHostUuid;
    private String dstHostUuid;
    private String CpuXml;

    public String getCpuXml() {
        return CpuXml;
    }

    public void setCpuXml(String cpuXml) {
        CpuXml = cpuXml;
    }

    @Override
    public String getHostUuid() {
        return dstHostUuid;
    }

    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }

    public String getDstHostUuid() {
        return dstHostUuid;
    }

    public void setDstHostUuid(String dstHostUuid) {
        this.dstHostUuid = dstHostUuid;
    }
}
