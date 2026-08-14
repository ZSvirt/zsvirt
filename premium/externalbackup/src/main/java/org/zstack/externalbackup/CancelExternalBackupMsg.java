package org.zstack.externalbackup;

import org.zstack.header.message.CancelMessage;
/**
 * Created by MaJin on 2019/12/2.
 */
public class CancelExternalBackupMsg extends CancelMessage implements ExternalBackupMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getExternalBackupUuid() {
        return uuid;
    }
}
