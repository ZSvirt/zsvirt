package org.zstack.pciDevice;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.identity.AccountManager;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO_;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PciDeviceFilterFlow implements HostAllocatorFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PciDeviceFilterFlow.class);

    @Autowired
    private AccountManager acntMgr;

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        String vmUuid = spec.getVmInstance().getUuid();

        // filter candidate hosts by pci specs
        filterHostCandidatesByPciSpec(candidates, vmUuid);
        if (candidates.isEmpty()) {
            return;
        }

        // filter candidate hosts by pci devices
        List<PciDeviceVO> pciDeviceVOS = getPciDevicesForVmInstance(vmUuid);
        if (pciDeviceVOS.isEmpty()) {
            logger.debug(String.format("no pci device attached record or tag for vm[uuid:%s], " +
                    "so skip pci device host filter", vmUuid));
            return;
        }

        // STEP1: FILTER HOST CANDIDATES BY PCI DEVICES
        String attachedPciUuid = pciDeviceVOS.get(0).getUuid();
        String dstHostUuid = pciDeviceVOS.get(0).getHostUuid();
        logger.debug(String.format(
                "pci devices in host[uuid:%s] are attached to vm[uuid:%s], so vm can only start on this host",
                dstHostUuid, vmUuid));

        for (PciDeviceVO vo : pciDeviceVOS) {
            if (!vo.getHostUuid().equals(dstHostUuid)) {
                throw new OperationFailureException(operr("specified pci devices are not on the same host: pci device[uuid: %s] " +
                                "on host[uuid: %s] while pci device[uuid: %s] on host[uuid: %s]",
                        vo.getUuid(), vo.getHostUuid(), attachedPciUuid, dstHostUuid
                ));
            }
        }

        for (HostCandidate candidate : candidates) {
            if (!candidate.getUuid().equals(dstHostUuid)) {
                candidate.markAsRejected(getClass(), i18m("the PCI devices[uuid:%s] is not on this host", attachedPciUuid));
            }
        }

        candidates.removeIf(candidate -> candidate.reject != null);

        // STEP2: FILTER HOST CANDIDATES BY IOMMU STATE
        rejectHostCandidatesByIommuState(candidates);
    }

    private List<PciDeviceVO> getPciDevicesForVmInstance(String vmInstanceUuid) {
        // get pci devices that already attached to vm / reserved by vm
        List<PciDeviceVO> pciDeviceVOS = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, vmInstanceUuid)
                .list();

        // pci devices associated to vf nics are not handled here
        pciDeviceVOS = pciDeviceVOS.stream().filter(vo -> vo.getVirtStatus() != PciDeviceVirtStatus.SRIOV_VIRTUAL ||
                vo.getType() != PciDeviceType.Ethernet_Controller).collect(Collectors.toList());

        if (!pciDeviceVOS.isEmpty()) {
            return pciDeviceVOS;
        }

        return Collections.emptyList();
    }

    private void filterHostCandidatesByPciSpec(List<HostCandidate> candidates, String vmUuid) {
        Map<String, Integer> specMap = PciDeviceUtils.getVmPciSpecUuids(vmUuid);
        if (specMap.isEmpty()) {
            logger.debug(String.format("no pci device spec for vm[uuid:%s]", vmUuid));
            return;
        }

        long specNum = Q.New(PciDeviceSpecVO.class)
                .in(PciDeviceSpecVO_.uuid, specMap.keySet())
                .count();
        if (specMap.size() != specNum) {
            throw new OperationFailureException(operr(
                    "failed to start vm[uuid:%s] because not all pci specs[uuids:%s] exist", vmUuid, specMap.keySet())
            );
        }

        // find insufficient specs
        List<String> insufficientSpecs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : specMap.entrySet()) {
            long attached = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, vmUuid)
                    .eq(PciDeviceVO_.pciSpecUuid, entry.getKey())
                    .eq(PciDeviceVO_.chooser, PciDeviceChooser.Spec)
                    .count();
            if (attached < entry.getValue()) {
                insufficientSpecs.add(entry.getKey());
            }
        }
        if (insufficientSpecs.isEmpty()) {
            logger.debug(String.format("all spec related pci devices have been allocated for vm[uuid:%s]", vmUuid));
            return;
        }

        logger.debug(String.format("insufficient pci specs for vm[uuid:%s]: %s", vmUuid, insufficientSpecs));

        // FILTER HOST CANDIDATES BY IOMMU STATE
        rejectHostCandidatesByIommuState(candidates);
        candidates.removeIf(candidate -> candidate.reject != null);
        if (candidates.isEmpty()) {
            return;
        }

        // filter out hosts that cannot satisfy insufficient specs
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(vmUuid);
        for (String specUuid : insufficientSpecs) {
            Integer deviceNum = specMap.get(specUuid);
            for (HostCandidate host : candidates) {
                Q query = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.hostUuid, host.getUuid())
                        .eq(PciDeviceVO_.pciSpecUuid, specUuid)
                        .eq(PciDeviceVO_.state, PciDeviceState.Enabled)
                        .in(PciDeviceVO_.status, PciDeviceStatus.attachablePciDeviceStatus)
                        .in(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.attachablePciDeviceVirtStatus);
                if (!accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                    List<String> accessiblePciUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, PciDeviceVO.class);
                    if (CollectionUtils.isEmpty(accessiblePciUuids)) {
                        accessiblePciUuids = Collections.singletonList(Platform.FAKE_UUID);
                    }

                    query = query.in(PciDeviceVO_.uuid, accessiblePciUuids);
                }

                long availableNum = query.count();

                if (deviceNum > availableNum) {
                    host.markAsRejected(getClass(), i18m("not enough PCI devices"));
                }
            }
        }
        candidates.removeIf(candidate -> candidate.reject != null);
    }

    private void rejectHostCandidatesByIommuState(List<HostCandidate> candidates) {
        HostIommuGetter hostIommuGetter = new HostIommuGetter();
        for (HostCandidate candidate : candidates) {
            HostIommuStateType type = hostIommuGetter.getState(candidate.getUuid());
            if (!HostIommuStateType.Enabled.equals(type)) {
                candidate.markAsRejected(getClass(), i18m("IOMMU state is not enabled"));
            }

            HostIommuStatusType status = hostIommuGetter.getStatus(candidate.getUuid());
            if (!HostIommuStatusType.Active.equals(status)) {
                candidate.markAsRejected(getClass(), i18m("IOMMU status is not active"));
            }
        }
    }
}
