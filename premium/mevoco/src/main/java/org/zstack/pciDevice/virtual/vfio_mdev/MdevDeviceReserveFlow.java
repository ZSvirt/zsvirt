package org.zstack.pciDevice.virtual.vfio_mdev;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.CleanUpAfterVmFailedToStartExtensionPoint;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorReserveExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceBeforeStartExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MdevDeviceReserveFlow implements
        VmInstanceBeforeStartExtensionPoint,
        HostAllocatorReserveExtensionPoint,
        CleanUpAfterVmFailedToStartExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MdevDeviceReserveFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public HostAllocatorReserveExtensionPoint getExtension() {
        return new MdevDeviceReserveFlow();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
        HostAllocatorSpec spec = (HostAllocatorSpec) data.get(HostAllocatorConstant.Param.SPEC);

        String vmUuid = spec.getVmInstance().getUuid();
        String hostUuid = host.getUuid();

        Map<String, Integer> specMap = MdevDeviceUtils.getVmMdevSpecUuids(vmUuid);
        if (specMap.isEmpty()) {
            trigger.next();
            return;
        }

        CheckAndReserveMdevDeviceBySpecMsg cmsg = new CheckAndReserveMdevDeviceBySpecMsg();
        cmsg.setHostUuid(hostUuid);
        cmsg.setVmUuid(vmUuid);
        cmsg.setDryRun(spec.isDryRun());
        bus.makeTargetServiceIdByResourceUuid(cmsg, MdevDeviceConstants.SERVICE_ID, hostUuid);
        bus.send(cmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                } else {
                    CheckAndReserveMdevDeviceBySpecReply rly = reply.castReply();
                    if (!rly.isSuccess()) {
                        trigger.fail(rly.getError());
                    } else {
                        data.put("vm-uuid", vmUuid);
                        data.put("reserved-mdev-devices", rly.getReservedMdevDevices());
                        trigger.next();
                    }
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        String vmUuid = (String) data.get("vm-uuid");
        List<String> reservedMdevDevices = (List<String>) data.get("reserved-mdev-devices");
        if (CollectionUtils.isEmpty(reservedMdevDevices)) {
            trigger.rollback();
            return;
        }

        MdevDeviceUtils.detachMdevDeviceFromVmInDB(reservedMdevDevices);
        trigger.rollback();
    }

    @Override
    public void cleanUpAfterVmFailedToStart(VmInstanceInventory inv, VmOperation op) {
        MdevDeviceUtils.detachMdevDeviceForVmInDB(inv.getUuid(), null, MdevDeviceStatus.Reserved, null);
    }
    
    @Override
    public ErrorCode handleSystemTag(String vmUuid, List<String> tags) {
        // Call when creating vm with mdev device reservation tag (after creating reservation system tag)
        // 1. Delete all reservation tags on the vm
        // 2. Check the mdev devices state
        // 3. Change mdev devices status : Active => Reserved
        List<String> reservationMdevUuids = tags.stream()
                .filter(tag -> MdevDeviceSystemTags.MDEV_DEVICE.isMatch(tag))
                .map(tag -> MdevDeviceSystemTags.MDEV_DEVICE.getTokensByTag(tag))
                .map(token -> token.get(MdevDeviceSystemTags.MDEV_DEVICE_TOKEN))
                .collect(Collectors.toList());
        if (reservationMdevUuids.isEmpty()) {
            return null;
        }
        MdevDeviceSystemTags.MDEV_DEVICE.delete(vmUuid);
    
        List<MdevDeviceVO> mdevs = Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.uuid, reservationMdevUuids)
                .list();
        List<String> wrongStatusMdevUuids = mdevs.stream()
                .filter(mdev -> !mdev.getStatus().isAttachable() ||
                        !MdevDeviceState.Enabled.equals(mdev.getState()))
                .map(MdevDeviceVO::getUuid)
                .collect(Collectors.toList());
        if (!wrongStatusMdevUuids.isEmpty()) {
            return operr(
                "mdev device[uuid:%s] can not attach to vm[uuid:%s] due to wrong status", wrongStatusMdevUuids, vmUuid);
        }
    
        mdevs.forEach(mdev -> {
            mdev.setStatus(MdevDeviceStatus.Reserved);
            mdev.setChooser(MdevDeviceChooser.Device);
            mdev.setVmInstanceUuid(vmUuid);
        });
        dbf.updateCollection(mdevs);
        logger.debug(String.format("mdev device[uuid:%s] status change: Active => Reserved; vmUuid : <any> => %s",
                mdevs.stream().map(MdevDeviceVO::getUuid).collect(Collectors.toList()), vmUuid));
        return null;
    }
}
