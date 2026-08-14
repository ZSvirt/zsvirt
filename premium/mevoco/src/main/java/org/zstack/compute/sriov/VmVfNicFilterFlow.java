package org.zstack.compute.sriov;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.sriov.VmVfNicManager;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.identity.AccountManager;
import org.zstack.pciDevice.HostIommuGetter;
import org.zstack.pciDevice.HostIommuStateType;
import org.zstack.pciDevice.HostIommuStatusType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmVfNicFilterFlow implements HostAllocatorFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmVfNicFilterFlow.class);
    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private VmVfNicManager vfMgr;

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        String vmUuid = spec.getVmInstance().getUuid();

        List<String> l3Uuids;
        List<String> hostUuids = new ArrayList<>();
        if (VmInstanceConstant.VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            l3Uuids = vfPciDeviceUtils.getL3UuidsFromVmNicParams(spec.getVmNicParams());
            l3Uuids.addAll(vfPciDeviceUtils.getL3UuidsNeedVdpa(spec.getL3NetworkUuids()));
            hostUuids.addAll(vfPciDeviceUtils.getHostUuidsFromVmNicParams(spec.getVmNicParams()));
        } else {
            l3Uuids = vfPciDeviceUtils.getL3UuidsWithVfNic(vmUuid);
        }

        if (l3Uuids.isEmpty()) {
            return;
        }

        if (!hostUuids.isEmpty()) {
            if (hostUuids.stream().distinct().count() > 1) {
                throw new OperationFailureException(err(HostAllocatorError.NO_AVAILABLE_HOST,
                        "vm[uuid:%s] could not have vf nic pci devices on different hosts", vmUuid));
            }

            for (HostCandidate candidate : candidates) {
                if (!hostUuids.contains(candidate.getUuid())) {
                    candidate.markAsRejected(getClass().getSimpleName(), i18m("vf nic pci devices required"));
                }
            }
            candidates.removeIf(candidate -> candidate.reject != null);
        }

        // filter host candidates by iommu state
        HostIommuGetter hostIommuGetter = new HostIommuGetter();
        for (HostCandidate candidate : candidates) {
            HostIommuStateType type = hostIommuGetter.getState(candidate.getUuid());
            if (!HostIommuStateType.Enabled.equals(type)) {
                candidate.markAsRejected(getClass().getSimpleName(), i18m("IOMMU state is not enabled"));
            }

            HostIommuStatusType status = hostIommuGetter.getStatus(candidate.getUuid());
            if (!HostIommuStatusType.Active.equals(status)) {
                candidate.markAsRejected(getClass().getSimpleName(), i18m("IOMMU status is not active"));
            }
        }
        candidates.removeIf(candidate -> candidate.reject != null);

        for (HostCandidate candidate : candidates) {
            for (String l3Uuid : l3Uuids) {
                if (!vfPciDeviceUtils.hasAvailableVfDevice(candidate.getUuid(), vmUuid, l3Uuid)) {
                    candidate.markAsRejected(getClass(),
                            i18m("no available vf nic pci device on l3[uuid:%s] for vm[uuid:%s] in this host",
                                    l3Uuid, vmUuid));
                    break;
                }
            }
        }
    }
}
