package org.zstack.drs.message;

import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2019/12/13.
 */
public class CreateDRSVmMigrationActivityReply extends MessageReply {
    private String activityUuid;

    public String getActivityUuid() {
        return activityUuid;
    }

    public void setActivityUuid(String activityUuid) {
        this.activityUuid = activityUuid;
    }
}
