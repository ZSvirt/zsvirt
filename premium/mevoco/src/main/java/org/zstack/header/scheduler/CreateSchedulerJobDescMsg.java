package org.zstack.header.scheduler;


import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APICreateMessage;
import org.zstack.identity.Session;

import java.util.Map;

/**
 * Created by root on 8/3/16.
 */
public interface CreateSchedulerJobDescMsg {
    public String getName();

    public String getDescription();

    public String getType();

    public Map<String, String> getParameters();

    public String getAccountUuid();

    public String getResourceUuid();

    default String getTargetResourceUuid() {
        return null;
    }
}
