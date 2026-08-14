package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @author hanyu.liang
 * @date 2023/9/15 14:42
 */
public class CreateCpuFeaturesHistoryMsg extends NeedReplyMessage implements HostMessage {
    private String srcHostUuid;
    private String dstHostUuid;
    private String srcCpuModelName;
    private boolean supportLiveMigration;

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

    public String getSrcCpuModelName() {
        return srcCpuModelName;
    }

    public void setSrcCpuModelName(String srcCpuModelName) {
        this.srcCpuModelName = srcCpuModelName;
    }

    public boolean isSupportLiveMigration() {
        return supportLiveMigration;
    }

    public void setSupportLiveMigration(boolean supportLiveMigration) {
        this.supportLiveMigration = supportLiveMigration;
    }

    @Override
    public String getHostUuid() {
        return srcHostUuid;
    }
}

