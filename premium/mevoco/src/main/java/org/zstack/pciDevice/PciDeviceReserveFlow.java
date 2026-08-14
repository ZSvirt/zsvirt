package org.zstack.pciDevice;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.CleanUpAfterVmChangeImageExtensionPoint;
import org.zstack.compute.vm.CleanUpAfterVmFailedToStartExtensionPoint;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.UpdateQuery;
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
public class PciDeviceReserveFlow implements
        VmInstanceBeforeStartExtensionPoint,
        HostAllocatorReserveExtensionPoint,
        CleanUpAfterVmFailedToStartExtensionPoint,
        CleanUpAfterVmChangeImageExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PciDeviceReserveFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public HostAllocatorReserveExtensionPoint getExtension() {
        return new PciDeviceReserveFlow();
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
        HostAllocatorSpec spec = (HostAllocatorSpec) data.get(HostAllocatorConstant.Param.SPEC);

        String vmUuid = spec.getVmInstance().getUuid();
        String hostUuid = host.getUuid();

        Map<String, Integer> specMap = PciDeviceUtils.getVmPciSpecUuids(vmUuid);
        if (specMap.isEmpty()) {
            trigger.next();
            return;
        }

        CheckAndReservePciDeviceBySpecMsg cmsg = new CheckAndReservePciDeviceBySpecMsg();
        cmsg.setHostUuid(hostUuid);
        cmsg.setVmUuid(vmUuid);
        cmsg.setDryRun(spec.isDryRun());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PciDeviceConstants.SERVICE_ID, cmsg.getHostUuid());
        bus.send(cmsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                } else {
                    CheckAndReservePciDeviceBySpecReply rly = reply.castReply();
                    if (!rly.isSuccess()) {
                        trigger.fail(rly.getError());
                    } else {
                        data.put("vm-uuid", vmUuid);
                        data.put("reserved-pci-devices", rly.getReservedPciDevices());
                        trigger.next();
                    }
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        String vmUuid = (String) data.get("vm-uuid");
        List<String> reservedPciDevices = (List<String>) data.get("reserved-pci-devices");
        if (CollectionUtils.isEmpty(reservedPciDevices)) {
            trigger.rollback();
            return;
        }

        PciDeviceUtils.cleanUpReserveTags(vmUuid, reservedPciDevices);
        for (String pciUuid : reservedPciDevices) {
            UpdateQuery.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                    .eq(PciDeviceVO_.uuid, pciUuid)
                    .set(PciDeviceVO_.status, PciDeviceUtils.getAvailableStatus(pciUuid))
                    .set(PciDeviceVO_.vmInstanceUuid, null)
                    .set(PciDeviceVO_.chooser, PciDeviceChooser.None)
                    .update();
        }

        trigger.rollback();
    }

    @Override
    public void cleanUpAfterVmFailedToStart(VmInstanceInventory inv, VmOperation op) {
        // Change pci devices status : Reserved => Active / System
        List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, inv.getUuid())
                .eq(PciDeviceVO_.status, PciDeviceStatus.Reserved)
                .notEq(PciDeviceVO_.type,  PciDeviceType.Ethernet_Controller)
                .list();
        if (pcis.isEmpty()) {
            return;
        }
        pcis.forEach(pci -> {
            pci.setVmInstanceUuid(null);
            pci.setStatus(PciDeviceUtils.getAvailableStatus(pci.getUuid()));
            pci.setChooser(PciDeviceChooser.None);
        });
        dbf.updateCollection(pcis);
    }

    public void cleanUpAfterVmChangeImage(VmInstanceInventory inv) {
    }

    @Override
    public ErrorCode handleSystemTag(String vmUuid, List<String> tags) {
        // Call when creating vm with pci device reservation tag (after creating reservation system tag)
        // 1. Delete all reservation tags on the vm
        // 2. Check the pci devices state
        // 3. Change pci devices status : Active / System => Reserved
        List<String> reservationPciUuids = tags.stream()
                .filter(tag -> PciDeviceSystemTags.PCI_DEVICE.isMatch(tag))
                .map(tag -> PciDeviceSystemTags.PCI_DEVICE.getTokensByTag(tag))
                .map(token -> token.get(PciDeviceSystemTags.PCI_DEVICE_TOKEN))
                .collect(Collectors.toList());
        if (reservationPciUuids.isEmpty()) {
            return null;
        }
        PciDeviceSystemTags.PCI_DEVICE.delete(vmUuid);

        List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.uuid, reservationPciUuids)
                .list();
        List<String> wrongStatusPciUuids = pcis.stream()
                .filter(pci -> !pci.getStatus().isAttachable() ||
                        !pci.getVirtStatus().isAttachable() ||
                        !PciDeviceState.Enabled.equals(pci.getState()))
                .map(PciDeviceVO::getUuid)
                .collect(Collectors.toList());
        if (!wrongStatusPciUuids.isEmpty()) {
            return operr(
                "pci device[uuid:%s] can not attach to vm[uuid:%s] due to wrong status", wrongStatusPciUuids, vmUuid);
        }

        pcis.forEach(pci -> {
            pci.setStatus(PciDeviceStatus.Reserved);
            pci.setChooser(PciDeviceChooser.Device);
            pci.setVmInstanceUuid(vmUuid);
        });
        dbf.updateCollection(pcis);
        logger.debug(String.format("pci device[uuid:%s] status change: Active/System => Reserved; vmUuid : <any> => %s",
                pcis.stream().map(PciDeviceVO::getUuid).collect(Collectors.toList()), vmUuid));
        return null;
    }
}
