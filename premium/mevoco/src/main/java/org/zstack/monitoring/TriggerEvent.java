package org.zstack.monitoring;

import org.zstack.header.core.Completion;
import org.zstack.monitoring.actions.MonitorTriggerActionInventory;

/**
 * Created by xing5 on 2017/6/12.
 */
public abstract class TriggerEvent {
    protected MonitorTriggerInventory trigger;
    protected MonitorTriggerActionInventory action;
    protected MonitorTriggerContext context;

    public TriggerEvent(MonitorTriggerInventory trigger, MonitorTriggerActionInventory action, MonitorTriggerContext context) {
        this.trigger = trigger;
        this.action = action;
        this.context = context;
    }

    public abstract void triggerOK(Completion completion);

    public abstract void triggerProblem(Completion completion);

    public MonitorTriggerInventory getTrigger() {
        return trigger;
    }

    public void setTrigger(MonitorTriggerInventory trigger) {
        this.trigger = trigger;
    }

    public MonitorTriggerActionInventory getAction() {
        return action;
    }

    public void setAction(MonitorTriggerActionInventory action) {
        this.action = action;
    }

    public MonitorTriggerContext getContext() {
        return context;
    }

    public void setContext(MonitorTriggerContext context) {
        this.context = context;
    }
}
