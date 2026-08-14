package org.zstack.storage.primary.block.message;

import org.zstack.storage.primary.local.InitPrimaryStorageOnHostConnectedMsg;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/11/20 16:47
 */
public class InitBlockPrimaryStorageOnHostConnectedMsg extends InitPrimaryStorageOnHostConnectedMsg {
    private Boolean isNewAdded;
    private String initiatorName;
    private String metadata;

    public Boolean getNewAdded() {
        return isNewAdded;
    }

    public void setNewAdded(Boolean newAdded) {
        isNewAdded = newAdded;
    }

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadata() {
        return metadata;
    }
}
