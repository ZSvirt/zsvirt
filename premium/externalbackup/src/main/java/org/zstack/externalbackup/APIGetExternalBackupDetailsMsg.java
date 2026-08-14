package org.zstack.externalbackup;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

public abstract class APIGetExternalBackupDetailsMsg extends APISyncCallMessage implements ExternalBackupMessage {
    @APIParam(resourceType = ExternalBackupVO.class)
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

