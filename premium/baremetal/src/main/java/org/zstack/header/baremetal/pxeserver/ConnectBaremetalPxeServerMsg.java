package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-11.
 */
public class ConnectBaremetalPxeServerMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String uuid;
    private boolean newAdd = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isNewAdd() {
        return newAdd;
    }

    public void setNewAdd(boolean newAdd) {
        this.newAdd = newAdd;
    }

    @Override
    public String getPxeServerUuid() {
        return uuid;
    }
}
