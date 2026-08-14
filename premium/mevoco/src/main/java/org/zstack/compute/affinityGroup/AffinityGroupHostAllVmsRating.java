package org.zstack.compute.affinityGroup;

import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AffinityGroupHostAllVmsRating implements AffinityGroupRatingFactory {

    private static final CLogger logger = Utils.getLogger(AffinityGroupHostAllVmsRating.class);

    @Override
    public AffinityGroupType getAffinityGroupType() {
        return AffinityGroupType.HOSTALL;
    }

    @Override
    public Map<String, Long> ratingHostCandidates(AffinityGroupRatingStruct struct) {
        Map<String, Long> hostWithVm = new SQLBatchWithReturn<Map<String, Long>>() {
            @Override
            protected Map<String, Long> scripts() {
                List<String> vmUuids = Q.New(AffinityGroupUsageVO.class)
                        .eq(AffinityGroupUsageVO_.affinityGroupUuid, struct.getAffinityGroupUuid())
                        .select(AffinityGroupUsageVO_.resourceUuid).listValues();
                /* don't count current vm */
                vmUuids.remove(struct.getSpec().getVmInstance().getUuid());

                Map<String, Long> hostVmsMap = new HashedMap();
                if (vmUuids.isEmpty()) {
                    return hostVmsMap;
                }

                List<VmInstanceVO> vmsWithoutHostUuid = new ArrayList<>();

                /* get hostUuid of VmInstance */
                List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).in(VmInstanceVO_.uuid, vmUuids).list();
                for (VmInstanceVO vo : vms) {
                    if (vo.getHostUuid() != null) {
                        if (hostVmsMap.get(vo.getHostUuid()) == null) {
                            hostVmsMap.put(vo.getHostUuid(), 1L);
                        } else {
                            hostVmsMap.put(vo.getHostUuid(), hostVmsMap.get(vo.getHostUuid()) + 1);
                        }
                    } else {
                        vmsWithoutHostUuid.add(vo);
                    }
                }

                /* get hostUuid of vm root volume */
                for (VmInstanceVO vo : vmsWithoutHostUuid) {
                    if (vo.getRootVolumeUuid() == null) {
                        continue;
                    }

                    List<String> hostUuids = Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, vo.getRootVolumeUuid())
                            .eq(LocalStorageResourceRefVO_.resourceType, VolumeVO.class.getSimpleName())
                            .select(LocalStorageResourceRefVO_.hostUuid).listValues();
                    if (hostUuids == null || hostUuids.isEmpty()) {
                        continue;
                    }

                    String hostUuid = hostUuids.get(0);
                    if (hostVmsMap.get(hostUuid) == null) {
                        hostVmsMap.put(hostUuid, 1L);
                    } else {
                        hostVmsMap.put(hostUuid, hostVmsMap.get(hostUuid) + 1);
                    }
                }

                return hostVmsMap;
            }
        }.execute();

        for (HostInventory h : struct.candidates) {
            if (!hostWithVm.keySet().contains(h.getUuid())) {
                hostWithVm.put(h.getUuid(), 0L);
            }
        }

        return hostWithVm;
    }
}
