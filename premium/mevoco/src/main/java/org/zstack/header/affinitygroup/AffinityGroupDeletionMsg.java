package org.zstack.header.affinitygroup;

import org.zstack.header.message.DeletionMessage;

/**
 * Created by shixin on 2017-11-16.
 */
public class AffinityGroupDeletionMsg extends DeletionMessage implements AffinityGroupMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAffinityGroupUuid() {
        return uuid;
    }
}
