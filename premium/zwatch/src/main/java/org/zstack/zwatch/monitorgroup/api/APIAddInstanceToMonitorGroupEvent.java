package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.zwatch.alarm.AlarmStatus;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInstanceInventory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInstanceVO;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupVO;

@RestResponse(allTo = "inventory")
public class APIAddInstanceToMonitorGroupEvent extends APIEvent {

    private MonitorGroupInstanceInventory inventory;

    public static APIAddInstanceToMonitorGroupEvent __example__() {
        APIAddInstanceToMonitorGroupEvent ret = new APIAddInstanceToMonitorGroupEvent();
        MonitorGroupInstanceInventory inventory = new MonitorGroupInstanceInventory();
        inventory.setGroupUuid(uuid(MonitorGroupVO.class));
        inventory.setUuid(uuid(MonitorGroupInstanceVO.class));
        inventory.setInstanceResourceType(VmInstanceVO.class.getSimpleName());
        inventory.setInstanceUuid(uuid(ResourceVO.class));
        inventory.setStatus(AlarmStatus.OK);
        inventory.setCreateDate(DocUtils.timestamp());
        inventory.setLastOpDate(DocUtils.timestamp());
        ret.setInventory(inventory);
        return ret;
    }

    public APIAddInstanceToMonitorGroupEvent() {
    }

    public MonitorGroupInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorGroupInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddInstanceToMonitorGroupEvent(String apiId) {
        super(apiId);
    }
}
