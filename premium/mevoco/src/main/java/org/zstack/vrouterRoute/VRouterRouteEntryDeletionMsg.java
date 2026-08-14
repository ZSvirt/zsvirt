package org.zstack.vrouterRoute;

import org.zstack.header.message.DeletionMessage;

/**
 * Created by weiwang on 21/06/2017.
 */
public class VRouterRouteEntryDeletionMsg extends DeletionMessage {
    private String uuid;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}
