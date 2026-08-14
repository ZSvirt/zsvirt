package org.zstack.pciDevice.virtual.vfio_mdev;

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
import org.zstack.pciDevice.HostIommuGetter;
import org.zstack.pciDevice.HostIommuStateType;
import org.zstack.pciDevice.HostIommuStatusType;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.*;
import static org.zstack.core.Platform.i18n;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MdevDeviceFilterFlow implements HostAllocatorFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MdevDeviceFilterFlow.class);

    @Autowired
    private AccountManager acntMgr;

    private void filterHostCandidatesByMdevSpec(List<HostCandidate> candidates, String vmUuid) {
        Map<String, Integer> specMap = MdevDeviceUtils.getVmMdevSpecUuids(vmUuid);
        if (specMap.isEmpty()) {
            logger.debug(String.format("no need to filter candidate hosts based on mdev specs because vm[uuid:%s] doesn't have any", vmUuid));
            return;
        }

        long specNum = Q.New(MdevDeviceSpecVO.class)
                .in(MdevDeviceSpecVO_.uuid, specMap.keySet())
                .count();
        if (specMap.size() != specNum) {
            throw new OperationFailureException(operr(
                    "failed to start vm[uuid:%s] because not all mdev specs[uuids:%s] exist", vmUuid, specMap.keySet())
            );
        }

        List<String> insufficientSpecs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : specMap.entrySet()) {
            long attached = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                    .eq(MdevDeviceVO_.mdevSpecUuid, entry.getKey())
                    .eq(MdevDeviceVO_.chooser, MdevDeviceChooser.Spec)
                    .count();
            if (attached < entry.getValue()) {
                insufficientSpecs.add(entry.getKey());
            }
        }
        if (insufficientSpecs.isEmpty()) {
            logger.debug(String.format("all spec related mdev devices have been allocated for vm[uuid:%s]", vmUuid));
            return;
        }

        logger.debug(String.format("insufficient mdev specs of vm[uuid:%s]: %s", vmUuid, insufficientSpecs));

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
                Q query = Q.New(MdevDeviceVO.class)
                        .eq(MdevDeviceVO_.hostUuid, host.getUuid())
                        .eq(MdevDeviceVO_.mdevSpecUuid, specUuid)
                        .eq(MdevDeviceVO_.state, MdevDeviceState.Enabled)
                        .in(MdevDeviceVO_.status, MdevDeviceStatus.attachableMdevDeviceStatus);

                if (!accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                    List<String> accessibleMdevUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, MdevDeviceVO.class);
                    if (CollectionUtils.isEmpty(accessibleMdevUuids)) {
                        accessibleMdevUuids = Collections.singletonList(Platform.FAKE_UUID);
                    }

                    query = query.in(MdevDeviceVO_.uuid, accessibleMdevUuids);
                }

                long availableNum = query.count();

                if (deviceNum > availableNum) {
                    host.markAsRejected(getClass(), i18m("not enough Mdev devices"));
                }
            }
        }
        candidates.removeIf(candidate -> candidate.reject != null);
    }

    /*
     * Filter candidate hosts by mdev devices that are assigned to vm,
     * both new devices specified by MdevDevice tag, or already assigned devices recorded in database.
     */
    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        String vmUuid = spec.getVmInstance().getUuid();

        // filter candidate hosts by mdev specs
        filterHostCandidatesByMdevSpec(candidates, vmUuid);
        if (candidates.isEmpty()) {
            return;
        }

        // all MdevDeviceVO attached to vm / reserved by vm
        List<MdevDeviceVO> mdevs = Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, vmUuid).list();
        if (mdevs.isEmpty()) {
            logger.debug(String.format("no mdev device attached record or tag for vm[uuid:%s], " +
                    "so skip mdev device host filter", vmUuid));
            return;
        }

        // STEP1: FILTER HOST CANDIDATES BY MDEV DEVICES
        String attachedMdevUuid = mdevs.get(0).getUuid();
        String dstHostUuid = mdevs.get(0).getHostUuid();
        logger.debug(String.format(
                "mdev devices in host[uuid:%s] are attached to vm[uuid:%s], so vm can only start on this host",
                dstHostUuid, vmUuid));

        for (MdevDeviceVO mdev : mdevs) {
            if (!mdev.getHostUuid().equals(dstHostUuid)) {
                throw new OperationFailureException(operr("specified mdev devices not on same host: mdev device[uuid: %s] " +
                                "on host[uuid: %s] while mdev device[uuid: %s] on host[uuid: %s]",
                        mdev.getUuid(), mdev.getHostUuid(), attachedMdevUuid, dstHostUuid
                ));
            }
        }

        for (HostCandidate candidate : candidates) {
            if (!candidate.getUuid().equals(dstHostUuid)) {
                candidate.markAsRejected(getClass(), i18m("the Mdev devices[uuid:%s] is not on this host", attachedMdevUuid));
            }
        }

        candidates.removeIf(candidate -> candidate.reject != null);

        // STEP2: FILTER HOST CANDIDATES BY IOMMU STATE
        // SE mdev device not check IOMMU
        if (!Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, vmUuid).notEq(MdevDeviceVO_.type, MdevDeviceType.SE_Controller).isExists()){
            logger.debug(String.format("SE mdev device attached record or tag for vm[uuid:%s], " +
                    "so skip mdev device host filter with Iommu status", vmUuid));
            return;
        }

        rejectHostCandidatesByIommuState(candidates);
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
