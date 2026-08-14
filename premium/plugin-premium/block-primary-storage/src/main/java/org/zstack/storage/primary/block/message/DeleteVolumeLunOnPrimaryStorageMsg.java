package org.zstack.storage.primary.block.message;

import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/8/2 10:23
 */
public class DeleteVolumeLunOnPrimaryStorageMsg extends DeleteVolumeBitsOnPrimaryStorageMsg {
    private String lunName;
    private Integer lunId;

    public Integer getLunId() {
        return lunId;
    }

    public String getLunName() {
        return lunName;
    }

    public void setLunId(Integer lunId) {
        this.lunId = lunId;
    }

    public void setLunName(String lunName) {
        this.lunName = lunName;
    }

    @Override
    public Class getReplayableClass() {
        return DeleteVolumeLunOnPrimaryStorageMsg.class;
    }
}
