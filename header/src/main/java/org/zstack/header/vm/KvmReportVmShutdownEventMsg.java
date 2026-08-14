package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/3/6 13:42
 */
public class KvmReportVmShutdownEventMsg extends NeedReplyMessage implements VmInstanceMessage{
    private String vmInstanceUuid;
    private String detail;
    private String opaque;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getOpaque() {
        return opaque;
    }

    public void setOpaque(String opaque) {
        this.opaque = opaque;
    }
}
