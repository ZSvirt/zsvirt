package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.zwatch.alarm.APICreateAlarmMsg;
import org.zstack.zwatch.alarm.AlarmActionVO;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventory")
public class APICreateMonitorGroupEvent extends APIEvent {

    private MonitorGroupInventory inventory;

    public static APICreateMonitorGroupEvent __example__() {
        APICreateMonitorGroupEvent ret = new APICreateMonitorGroupEvent();
        MonitorGroupInventory inventory = new MonitorGroupInventory();
        inventory.setUuid(uuid());
        inventory.setName("vm-group-1");
        inventory.setDescription("desc");
        APICreateAlarmMsg.ActionParam action = new APICreateAlarmMsg.ActionParam();
        action.actionType = SNSActionFactory.type.toString();
        action.actionUuid = uuid(AlarmActionVO.class);
        inventory.setActions(JSONObjectUtil.toJsonString(asList(action)));
        inventory.setLastOpDate(DocUtils.timestamp());
        inventory.setCreateDate(DocUtils.timestamp());
        ret.setInventory(inventory);
        return ret;
    }

    public APICreateMonitorGroupEvent() {
    }

    public MonitorGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateMonitorGroupEvent(String apiId) {
        super(apiId);
    }
}
