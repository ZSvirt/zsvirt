package org.zstack.monitoring.actions;

/**
 * Created by xing5 on 2017/7/7.
 */
public interface MonitorTriggerActionFactory {
    String getMonitorActionType();

    MonitorTriggerActionVO createMonitorTriggerAction(MonitorTriggerActionVO vo, APICreateMonitorTriggerActionMsg msg);

    MonitorTriggerActionInventory getInventory(String uuid);
}
