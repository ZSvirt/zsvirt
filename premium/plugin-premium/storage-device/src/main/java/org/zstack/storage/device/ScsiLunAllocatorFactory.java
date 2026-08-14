package org.zstack.storage.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storageDevice.*;
import org.zstack.storage.primary.local.LocalStorageAllocatorFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Create by weiwang at 2018/10/25
 */
public class ScsiLunAllocatorFactory implements HostAllocatorFilterExtensionPoint {
    private CLogger logger = Utils.getLogger(LocalStorageAllocatorFactory.class);
//
//    private HostAllocatorStrategyType type = new HostAllocatorStrategyType(StorageDeviceConstants.STORAGE_DEVICE_ALLOCATOR_STRATEGY, false);

    @Autowired
    private ErrorFacade errf;

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        List<ScsiLunVmInstanceRefVO> refVOS = Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, spec.getVmInstance().getUuid())
                .list();

        if (refVOS.isEmpty()) {
            return;
        }

        ScsiLunVO firstScsiLunVO = Q.New(ScsiLunVO.class)
                .eq(ScsiLunVO_.uuid, refVOS.get(0).getScsiLunUuid())
                .find();
        logger.debug(String.format(
                "there are storage devices[%s] attached on vm[uuid:%s], matched hosts is %s",
                refVOS.stream().map(ScsiLunVmInstanceRefVO::getScsiLunUuid).collect(Collectors.toList()),
                spec.getVmInstance().getUuid(),
                firstScsiLunVO));
        Set<String> firstHosts = transformToSet(firstScsiLunVO.getScsiLunHostRefs(), ScsiLunHostRefVO::getHostUuid);
        List<ScsiLunVO> scsiLuns = Q.New(ScsiLunVO.class)
                    .in(ScsiLunVO_.uuid, transform(refVOS, ScsiLunVmInstanceRefVO::getScsiLunUuid))
                    .list();

        for (ScsiLunVmInstanceRefVO vo : refVOS) {
            // scsiLunVO will never be null
            ScsiLunVO scsiLunVO = findOneOrNull(scsiLuns, lun -> lun.getUuid().equals(vo.getScsiLunUuid()));
            Set<String> hosts = transformToSet(scsiLunVO.getScsiLunHostRefs(), ScsiLunHostRefVO::getHostUuid);
            firstHosts.retainAll(hosts);
            if (hosts.isEmpty()) {
                throw new OperationFailureException(operr("scsi lun[uuid: %s] and [uuid: %s] does not has a common host",
                        firstScsiLunVO.getUuid(), scsiLunVO.getUuid()));
            }

            if (scsiLunVO.getState().equals(StorageDeviceState.Disabled.toString())) {
                throw new OperationFailureException(operr("scsi lun[uuid: %s] is in disabled state", scsiLunVO.getUuid()));
            }
        }

        for (HostCandidate candidate : candidates) {
            if (!firstHosts.contains(candidate.getUuid())) {
                candidate.markAsRejected(getClass(), i18m("the specific SCSI lun required"));
            }
        }
    }

//
//    @Override
//    public String getHostAllocatorStrategyName(HostAllocatorSpec spec) {
//        List<ScsiLunVmInstanceRefVO> refVOS = Q.New(ScsiLunVmInstanceRefVO.class)
//                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, spec.getVmInstance().getUuid())
//                .list();
//
//        if (refVOS == null || refVOS.isEmpty()) {
//            return null;
//        }
//
//        return StorageDeviceConstants.STORAGE_DEVICE_ALLOCATOR_STRATEGY;
//    }
//
//    @Override
//    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
//        return type;
//    }
}
