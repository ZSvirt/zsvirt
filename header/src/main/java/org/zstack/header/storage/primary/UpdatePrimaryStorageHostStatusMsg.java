package org.zstack.header.storage.primary;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Administrator on 2017-05-12.
 */
public class UpdatePrimaryStorageHostStatusMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String hostUuid;
    private PrimaryStorageHostStatus status;
    private ErrorCode reason;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public void setStatus(PrimaryStorageHostStatus status) {
        this.status = status;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public PrimaryStorageHostStatus getStatus() {
        return status;
    }

    public ErrorCode getReason() {
        return reason;
    }

    public void setReason(ErrorCode reason) {
        this.reason = reason;
    }
}
