package org.zstack.monitoring.items;

import org.zstack.monitoring.MonitorTriggerContext;

/**
 * Created by xing5 on 2017/6/16.
 */
public interface AlertTextWriter {
    String writeProblemAlertText(MonitorTriggerContext context);

    String writeOkAlertText(MonitorTriggerContext context);
}
