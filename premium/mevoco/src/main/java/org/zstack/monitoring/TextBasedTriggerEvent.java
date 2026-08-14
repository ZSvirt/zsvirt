package org.zstack.monitoring;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.monitoring.actions.MonitorTriggerActionInventory;
import org.zstack.monitoring.items.AlertTextWriter;
import org.zstack.monitoring.items.Item;

/**
 * Created by xing5 on 2017/6/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class TextBasedTriggerEvent extends TriggerEvent {
    @Autowired
    private MonitorManager mmgr;

    public TextBasedTriggerEvent(MonitorTriggerInventory trigger, MonitorTriggerActionInventory action, MonitorTriggerContext context) {
        super(trigger, action, context);
    }

    protected Item getItem() {
        return mmgr.getItemByExpressionString(trigger.getExpression());
    }

    protected AlertTextWriter createAlertTextWriter() {
        Item item = getItem();
        return item.createAlertTextWriter();
    }
}
