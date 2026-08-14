package org.zstack.compute.affinityGroup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AffinityGroupFilterFlow {
    private static final CLogger logger = Utils.getLogger(AffinityGroupFilterFlow.class);

    @Autowired
    private AffinityGroupManager agMgr;
    @Autowired
    private DatabaseFacade dbf;

    @Deprecated
    public String getAffinityGroupUuid(HostAllocatorSpec spec){
        String agUuid = AffinityGroupSystemTags.AFFINITY_GROUP_UUID.getTokenByResourceUuid(spec.getVmInstance().getUuid(), AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN);
        if (agUuid != null) {
            return agUuid;
        }

        if (spec.getSystemTags() == null || spec.getSystemTags().isEmpty()){
            return null;
        }


        Iterator<String > it = spec.getSystemTags().iterator();
        while (it.hasNext()){
            String tag = it.next();
            agUuid = AffinityGroupSystemTags.AFFINITY_GROUP_UUID.getTokenByTag(tag, AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN);
            if (agUuid != null){
                return agUuid;
            }
        }

        return null;
    }

    public AffinityGroupRatingFactory getRating(HostAllocatorSpec spec, String agUuid){
        AffinityGroupVO vo = dbf.findByUuid(agUuid, AffinityGroupVO.class);
        if(vo == null){
            /* wrong para */
            logger.debug(String.format("Affinity group [uuid: %s] for vm instance [uuid: %s] not found", agUuid, spec.getVmInstance().getUuid()));
            return null;
        }

        if (vo.getState() != AffinityGroupState.Enabled) {
            logger.debug(String.format("Affinity group [uuid: %s] state: %s is not enabled", agUuid, vo.getState().toString()));
            return null;
        }

        return agMgr.getAffinityGroupRating(vo.getType());
    }

    public Boolean checkVmSatisfyAffinityGroup(String VmUuid, String agUuid) {
        String hostUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, VmUuid).select(VmInstanceVO_.hostUuid).findValue();
        AffinityGroupInventory aginv = AffinityGroupInventory.valueOf(dbf.findByUuid(agUuid, AffinityGroupVO.class));
        if (aginv.isHardPolicy() && hostUuid != null) {
            HostAllocatorSpec spec = new HostAllocatorSpec();
            VmInstanceInventory vmInv = VmInstanceInventory.valueOf(dbf.findByUuid(VmUuid, VmInstanceVO.class));
            spec.setVmInstance(vmInv);
            HostVO hostvo = dbf.findByUuid(hostUuid, HostVO.class);

            AffinityGroupRatingFactory rating = agMgr.getAffinityGroupRating(AffinityGroupType.valueOf(aginv.getType()));
            List<HostVO> result = ratingHostCandidates(rating, asList(hostvo), spec, agUuid);
            return !result.isEmpty();
        }

        return true;
    }

    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec, String agUuid) {
        AffinityGroupRatingFactory rating = getRating(spec, agUuid);
        if (rating == null){
            return candidates;
        }
        logger.debug(String.format("start filter host on affinity group[uuid:%s]", agUuid));
        return ratingHostCandidates(rating, candidates, spec, agUuid);
    }

    private List<HostVO> ratingHostCandidates(AffinityGroupRatingFactory rating, List<HostVO> candidates, HostAllocatorSpec spec, String agUuid) {
        AffinityGroupRatingFactory.AffinityGroupRatingStruct struct = new AffinityGroupRatingFactory.AffinityGroupRatingStruct();
        struct.setSpec(spec);
        struct.setCandidates(HostInventory.valueOf(candidates));
        struct.setAffinityGroupUuid(agUuid);

        Map<String, Long> hostRating = rating.ratingHostCandidates(struct);

        AffinityGroupInventory inv = AffinityGroupInventory.valueOf(dbf.findByUuid(agUuid, AffinityGroupVO.class));
        if (inv.getPolicy().toString().equals(AffinityGroupPolicy.ANTIHARD.toString())){
            List<String> hostUuids = hostRating.entrySet().stream().filter(host -> host.getValue() == 0).map(host -> host.getKey()).collect(Collectors.toList());
            logger.debug(String.format("ratingHostCandidates[ANTIHARD] -> %s", candidates.stream().filter(vo -> hostUuids.contains(vo.getUuid())).map(HostVO::getUuid).collect(Collectors.toList())));
            return candidates.stream().filter(vo -> hostUuids.contains(vo.getUuid())).collect(Collectors.toList());
        }

        if(inv.getPolicy().toString().equals(AffinityGroupPolicy.AFFINITYHARD.toString())){
            List<String> hostUuids = hostRating.entrySet().stream().filter(host -> host.getValue() > 0).map(host -> host.getKey()).collect(Collectors.toList());
            if (hostUuids.isEmpty()) {
                return candidates;
            }
            logger.debug(String.format("ratingHostCandidates[AFFINITYHARD] -> %s", candidates.stream().filter(vo -> hostUuids.contains(vo.getUuid())).map(HostVO::getUuid).collect(Collectors.toList())));
            return candidates.stream().filter(vo -> hostUuids.contains(vo.getUuid())).collect(Collectors.toList());
        }

        return candidates;
    }

    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec) {
        String agUuid = getAffinityGroupUuid(spec);
        if (agUuid == null) {
            /* affinity Group is not set for this vm */
            return candidates;
        }

        return filterHostCandidates(candidates, spec, agUuid);
    }

    public String filterErrorReason() {
        return Platform.i18n("can not satisfied affinity group conditions");
    }
}
