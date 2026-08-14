package org.zstack.storage.primary.smp;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by xing5 on 2016/3/26.
 */
public class InitKvmHostMsg extends NeedReplyMessage implements PrimaryStorageMessage, SMPPrimaryStorageHypervisorSpecificMessage {
    private String primaryStorageUuid;
    private String hypervisorType;
    private String hostUuid;

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHypervisorType() {
        return hypervisorType;
    }
}
