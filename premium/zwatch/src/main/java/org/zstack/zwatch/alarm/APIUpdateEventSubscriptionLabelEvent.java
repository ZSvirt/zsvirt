package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

/**
 * Created by MaJin on 2019/12/11.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateEventSubscriptionLabelEvent extends APIEvent {
    private EventSubscriptionLabelInventory inventory;

    public APIUpdateEventSubscriptionLabelEvent() {
    }

    public APIUpdateEventSubscriptionLabelEvent(String apiId) {
        super(apiId);
    }

    public EventSubscriptionLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventSubscriptionLabelInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateEventSubscriptionLabelEvent __example__() {
        APIUpdateEventSubscriptionLabelEvent event = new APIUpdateEventSubscriptionLabelEvent();
        EventSubscriptionLabelInventory inventory = new EventSubscriptionLabelInventory();
        inventory.setUuid(uuid(EventSubscriptionLabelVO.class));
        inventory.setKey(VmNamespace.LabelNames.VMUuid.toString());
        inventory.setOperator(Label.Operator.Equal);
        inventory.setValue(uuid(VmInstanceVO.class));
        event.setInventory(inventory);
        return event;
    }
}