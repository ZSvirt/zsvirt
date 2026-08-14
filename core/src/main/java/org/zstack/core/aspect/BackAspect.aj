package org.zstack.core.aspect;

import org.zstack.utils.TaskContext;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public aspect BackAspect {
    private static final CLogger logger = Utils.getLogger(BackAspect.class);

    void around(org.zstack.header.core.workflow.Flow obj):target(obj) && execution(void Flow+.rollback(..)) {
        setMsgDeadline();
        proceed(obj);
    }

    void around(org.zstack.core.cloudbus.CloudBusCallBack obj):target(obj) && execution(void CloudBusCallBack+.CloudBusCallBack(..)) {
        setMsgDeadline();
        proceed(obj);
    }

    private void setMsgDeadline() {
        if (TaskContext.containsTaskContext("__messagedeadline__")) {
            long deadline = Long.parseLong((String) TaskContext.getTaskContext().get("__messagedeadline__"));
            if (deadline < System.currentTimeMillis()) {
                TaskContext.getTaskContext().put("__messagedeadline__", String.valueOf(deadline + TimeUtils.parseTimeInMillis("30m")));
                TaskContext.getTaskContext().put("__messagetimeout__", String.valueOf(TimeUtils.parseTimeInMillis("30m")));
            }
        }
    }
}