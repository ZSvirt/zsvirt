package org.zstack.monitoring.actions;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2017/6/11.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MonitorTriggerActionBase implements MonitorTriggerAction {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private MonitorTriggerActionVO self;

    public MonitorTriggerActionBase(MonitorTriggerActionVO self) {
        this.self = self;
    }

    private MonitorTriggerActionInventory getInventory() {
        return MonitorTriggerActionInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeMonitorTriggerActionStateMsg) {
            handle((APIChangeMonitorTriggerActionStateMsg) msg);
        } else if (msg instanceof APIDeleteMonitorTriggerActionMsg) {
            handle((APIDeleteMonitorTriggerActionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteMonitorTriggerActionMsg msg) {
        APIDeleteMonitorTriggerActionEvent evt = new APIDeleteMonitorTriggerActionEvent(msg.getId());
        dbf.remove(self);
        bus.publish(evt);
    }

    private void handle(APIChangeMonitorTriggerActionStateMsg msg) {
        MonitorTriggerActionStateEvent e = MonitorTriggerActionStateEvent.valueOf(msg.getStateEvent());
        if (e == MonitorTriggerActionStateEvent.enable) {
            self.setState(MonitorTriggerActionState.Enabled);
        } else {
            self.setState(MonitorTriggerActionState.Disabled);
        }

        self = dbf.updateAndRefresh(self);

        APIChangeMonitorTriggerActionStateEvent evt = new APIChangeMonitorTriggerActionStateEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }
}
