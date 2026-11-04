package org.zstack.ha;

import org.zstack.ha.entity.HaStrategyConditionInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


/**
 * @Author: DaoDao
 * @Date: 2023/4/4
 */
@RestResponse(allTo = "inventory")
public class APIUpdateHaStrategyConditionEvent extends APIEvent {
    private HaStrategyConditionInventory inventory;

    public APIUpdateHaStrategyConditionEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateHaStrategyConditionEvent() {
    }

    public HaStrategyConditionInventory getInventory() {
        return inventory;
    }

    public void setInventory(HaStrategyConditionInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateHaStrategyConditionEvent __example__() {
        APIUpdateHaStrategyConditionEvent event = new APIUpdateHaStrategyConditionEvent();
        event.setInventory(HaStrategyConditionInventory.__example__());
        return event;
    }
}
