package org.zstack.externalbackup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/12/3.
 */
public abstract class CreateExternalBackupMsg extends NeedReplyMessage {
    protected String resourceUuid;
    protected String name;
    protected String description;
    protected boolean dryRun;
    protected String longJobUuid;
    protected ExternalBackupSpec spec;

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public abstract String getType();

    public ExternalBackupSpec getSpec() {
        return spec;
    }

    public void setSpec(ExternalBackupSpec spec) {
        this.spec = spec;
    }

    public String getLongJobUuid() {
        return longJobUuid;
    }

    public void setLongJobUuid(String longJobUuid) {
        this.longJobUuid = longJobUuid;
    }

    public boolean allowResume() {
        return longJobUuid != null;
    }
}
