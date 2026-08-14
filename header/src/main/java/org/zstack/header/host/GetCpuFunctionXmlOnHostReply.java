package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2021/7/29 15:30
 */
public class GetCpuFunctionXmlOnHostReply extends MessageReply {
    private String cpuXml;
    private String cpuModelName;

    public String getCpuModelName() {
        return cpuModelName;
    }

    public void setCpuModelName(String cpuModelName) {
        this.cpuModelName = cpuModelName;
    }

    public String getCpuXml() {
        return cpuXml;
    }

    public void setCpuXml(String cpuXml) {
        this.cpuXml = cpuXml;
    }
}
