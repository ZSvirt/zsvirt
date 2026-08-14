package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.utils.CollectionUtils;

import java.util.*;

import static org.zstack.core.Platform.i18m;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedVolumePrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void allocate() {
        if (VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            next();
            return;
        }

        VmInstanceInventory vm = spec.getVmInstance();
        if (vm.getRootVolume() == null || !VolumeStatus.Ready.toString().equals(vm.getRootVolume().getStatus())) {
            fail(Platform.operr("cannot find root volume of vm[uuid:%s]", vm.getUuid()));
        }

        List<String> requiredPsUuids = CollectionUtils.transformAndRemoveNull(vm.getAllVolumes(), VolumeInventory::getPrimaryStorageUuid);

        // find out cluster that have all required primary storage attached
        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.IN, requiredPsUuids);
        List<PrimaryStorageClusterRefVO> refs = q.list();
        Map<String, Set<String>> clusterPs = new HashMap<>();
        for (PrimaryStorageClusterRefVO ref : refs) {
            Set<String> pss = clusterPs.computeIfAbsent(ref.getClusterUuid(), k -> new HashSet<>());
            pss.add(ref.getPrimaryStorageUuid());
        }

        List<String> clusterHavingAllPs = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : clusterPs.entrySet()) {
            if (e.getValue().containsAll(requiredPsUuids)) {
                clusterHavingAllPs.add(e.getKey());
            }
        }

        if (clusterHavingAllPs.isEmpty()) {
            rejectAll(i18m("need to attach primary storage: %s", requiredPsUuids));
            next();
            return;
        }

        // find out host in above result clusters
        for (HostCandidate candidate : candidates) {
            if (!clusterHavingAllPs.contains(candidate.host.getClusterUuid())) {
                reject(candidate, i18m("need to attach primary storage: %s", requiredPsUuids));
            }
        }

        next();
    }
}
