package org.zstack.externalbackup;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by MaJin on 2019/11/30.
 */
public abstract class APICreateExternalBackupMsg extends APICreateMessage {
    @APIParam
    protected String name;
    @APIParam(required = false)
    protected String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String getType();

    public abstract CreateExternalBackupMsg toLocalMessage();
}
