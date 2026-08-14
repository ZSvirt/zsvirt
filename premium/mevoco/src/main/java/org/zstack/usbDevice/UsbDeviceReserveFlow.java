package org.zstack.usbDevice;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorReserveExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.List;
import java.util.Map;


/**
 * @Author: DaoDao
 * @Date: 2023/9/6
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UsbDeviceReserveFlow implements HostAllocatorReserveExtensionPoint {
    @Autowired
    private CloudBus bus;

    @Override
    public HostAllocatorReserveExtensionPoint getExtension() {
        return new UsbDeviceReserveFlow();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        HostAllocatorSpec spec = (HostAllocatorSpec) data.get(HostAllocatorConstant.Param.SPEC);
        String vmUuid = spec.getVmInstance().getUuid();

        if (!VmSystemTags.VM_ATTACH_USB.hasTag(vmUuid)) {
            trigger.next();
            return;
        }

        CheckAndReserveUsbDeviceMsg msg = new CheckAndReserveUsbDeviceMsg();
        msg.setVmUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, UsbDeviceConstants.SERVICE_ID, vmUuid);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    CheckAndReserveUsbDeviceReply reply1 = reply.castReply();
                    data.put("reservedUsbDevices", reply1.getRevertUsbUuids());
                    trigger.next();
                    return;
                }
                trigger.fail(reply.getError());
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        List<String> reservedUsbuuids = (List<String>) data.get("reservedUsbDevices");

        if (CollectionUtils.isEmpty(reservedUsbuuids)) {
            trigger.rollback();
            return;
        }

        HostAllocatorSpec spec = (HostAllocatorSpec) data.get(HostAllocatorConstant.Param.SPEC);
        String vmUuid = spec.getVmInstance().getUuid();
        UpdateQuery.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, vmUuid)
                .in(UsbDeviceVO_.uuid, reservedUsbuuids)
                .set(UsbDeviceVO_.attachType, null)
                .set(UsbDeviceVO_.vmInstanceUuid, null)
                .update();

        trigger.rollback();
    }
}
