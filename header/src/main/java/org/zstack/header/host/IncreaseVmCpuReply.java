package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by AlanJager on 2017/5/3.
 */
public class IncreaseVmCpuReply extends MessageReply {
    private int cpuNum;

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

}

