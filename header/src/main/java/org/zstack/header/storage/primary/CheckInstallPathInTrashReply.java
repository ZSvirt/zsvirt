package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/25.
 */
public class CheckInstallPathInTrashReply extends MessageReply {
    private Long trashId;
    private String resourceUuid;
    private List<String> relatedTrashPaths;

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

    public List<String> getRelatedTrashPaths() {
        return relatedTrashPaths;
    }

    public void setRelatedTrashPaths(List<String> relatedTrashPaths) {
        this.relatedTrashPaths = relatedTrashPaths;
    }
}
