package org.zstack.compute.sriov;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.CleanUpAfterVmFailedToStartExtensionPoint;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorReserveExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.compute.host.ReserveEthernetVfMsg;
import org.zstack.compute.host.ReserveHostPciDeviceReply;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.identity.AccountManager;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmVfNicReserveFlow implements HostAllocatorReserveExtensionPoint,
        CleanUpAfterVmFailedToStartExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmVfNicReserveFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private AccountManager acntMgr;

    private final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Override
    public HostAllocatorReserveExtensionPoint getExtension() {
        return new VmVfNicReserveFlow();
    }

    private void vmReservePciDeviceForL3(String hostUuid, String vmUuid, List<String> l3Uuids, Map<String, String> l3PciDeviceMap,
                                         boolean releaseOldVf, ReturnValueCompletion<List<PciDeviceInventory>> completion) {
        ReserveEthernetVfMsg msg = new ReserveEthernetVfMsg();
        msg.setVmUuid(vmUuid);
        msg.setHostUuid(hostUuid);
        msg.setL3Uuids(l3Uuids);
        msg.setL3PciDeviceMap(l3PciDeviceMap);
        msg.setReleaseOldVf(releaseOldVf);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                ReserveHostPciDeviceReply r = reply.castReply();
                completion.success(r.getPciDevices());
            }
        });
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
        HostAllocatorSpec spec = (HostAllocatorSpec) data.get(HostAllocatorConstant.Param.SPEC);
        VmInstanceSpec vmSpec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<String> l3Uuids = new ArrayList<>();

        String vmUuid;
        String hostUuid;
        VmInstanceConstant.VmOperation operation;
        if (spec == null) {
            /* attach l3 to vm */
            vmUuid = vmSpec.getVmInventory().getUuid();
            hostUuid = vmSpec.getVmInventory().getHostUuid();
            if (hostUuid == null) {
                hostUuid = vmSpec.getVmInventory().getLastHostUuid();
            }
            operation = vmSpec.getCurrentVmOperation();
            l3Uuids = VmNicSpec.getL3UuidsOfSpec(vmSpec.getL3Networks());
        } else {
            vmUuid = spec.getVmInstance().getUuid();
            l3Uuids = spec.getL3NetworkUuids();
            hostUuid = host.getUuid();
            if (spec.getVmOperation() != null) {
                /* create, start, migrate vm */
                operation = VmInstanceConstant.VmOperation.valueOf(spec.getVmOperation());
                if (spec.getAllocatorStrategy() != null && spec.getAllocatorStrategy().equals(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE)) {
                    operation = VmInstanceConstant.VmOperation.Migrate;
                }
            } else {
                /* happened in change-vm-cpu */
                operation = VmInstanceConstant.VmOperation.Start;
            }
        }

        boolean releaseOldVf = true;

        List<String> newNicL3Uuids;
        List<VmNicParam> vmNicParams = new ArrayList<>();
        Map<String, String> l3PciDeviceMap = new HashMap<>();
        if (VmInstanceConstant.VmOperation.AttachNic == operation) {
            vmNicParams.addAll(VmNicSpec.getVmNicParamsOfSpec(vmSpec.getL3Networks()));
            newNicL3Uuids = vfPciDeviceUtils.getL3UuidsFromVmNicParams(vmNicParams);
            newNicL3Uuids.addAll(vfPciDeviceUtils.getL3UuidsNeedVdpa(l3Uuids));
            l3PciDeviceMap.putAll(vfPciDeviceUtils.getL3PciDeviceMapFromVmNicParams(vmNicParams));
        } else if (VmInstanceConstant.VmOperation.NewCreate == operation) {
            if (spec != null) {
                vmNicParams.addAll(spec.getVmNicParams());
            }
            newNicL3Uuids = vfPciDeviceUtils.getL3UuidsFromVmNicParams(vmNicParams);
            newNicL3Uuids.addAll(vfPciDeviceUtils.getL3UuidsNeedVdpa(l3Uuids));
            l3PciDeviceMap.putAll(vfPciDeviceUtils.getL3PciDeviceMapFromVmNicParams(vmNicParams));
        } else {
            newNicL3Uuids = vfPciDeviceUtils.getL3UuidsWithVfNic(vmUuid);
        }

        if (CollectionUtils.isEmpty(newNicL3Uuids)) {
            trigger.next();
            return;
        }

        if (VmInstanceConstant.VmOperation.Migrate == operation) {
            releaseOldVf = false;
        }

        vmReservePciDeviceForL3(hostUuid, vmUuid, newNicL3Uuids, l3PciDeviceMap, releaseOldVf, new ReturnValueCompletion<List<PciDeviceInventory>>(trigger) {
            @Override
            public void success(List<PciDeviceInventory> returnValue) {
                data.put("reservedPciDevice", returnValue);
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        List<PciDeviceInventory> returnValue = (List<PciDeviceInventory>) data.get("reservedPciDevice");
        if (returnValue == null) {
            trigger.rollback();
            return;
        }

        for (PciDeviceInventory pci : returnValue) {
            vfPciDeviceUtils.releaseVfDevice(pci);
        }

        trigger.rollback();
    }

    @Override
    public void cleanUpAfterVmFailedToStart(VmInstanceInventory inv, VmOperation op) {
        if (VmOperation.NewCreate != op) {
            return;
        }

        vfPciDeviceUtils.releaseVfDevice(inv);
    }
}
