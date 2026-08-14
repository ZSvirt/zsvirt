package org.zstack.compute.vm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.SkipVmTracer;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by LiangHanYu on 2021/6/22 15:34
 */
@SkipVmTracer(replyClass = ExecuteCrashStrategyReply.class)
public class ExecuteCrashStrategyMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private boolean skipReboot = true;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public boolean isSkipReboot() {
        return skipReboot;
    }

    public void setSkipReboot(boolean skipReboot) {
        this.skipReboot = skipReboot;
    }
}
