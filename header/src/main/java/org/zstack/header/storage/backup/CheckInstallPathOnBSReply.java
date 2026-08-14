package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2018/12/25.
 */
public class CheckInstallPathOnBSReply extends MessageReply {
    private Long trashId;
    private String resourceUuid;

    public Long getTrashId() {
        return trashId;
    }

    public void setTrashId(Long trashId) {
        this.trashId = trashId;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
