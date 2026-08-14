package org.zstack.compute.vmscheduling;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.affinityGroup.AffinityGroupFilterFlow;
import org.zstack.compute.affinityGroup.AffinityGroupRatingFactory;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.affinitygroup.AffinityGroupInventory;
import org.zstack.header.affinitygroup.AffinityGroupPolicy;
import org.zstack.header.affinitygroup.AffinityGroupState;
import org.zstack.header.affinitygroup.AffinityGroupVO;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vmscheduling.VMSchedulingRuleMode;
import org.zstack.header.vmscheduling.VMSchedulingRuleType;
import org.zstack.header.vmscheduling.VmSchedulingRuleRefVO;
import org.zstack.header.vmscheduling.VmSchedulingRuleVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSchedulingRuleSortFlow extends AbstractHostSortorFlow {
    private static final CLogger logger = Utils.getLogger(VmSchedulingRuleSortFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected AffinityGroupFilterFlow filterFlow;
    @Autowired
    protected VmSchedulingRuleFilterFlow ruleFilterFlow;

    @Override
    public void sort() {
        String vmGroupUuid = ruleFilterFlow.getVmSchedulingRuleGroupUuid(spec);
        if (vmGroupUuid == null) {
            subCandidates.clear();
            subCandidates.addAll(candidates);
            return;
        }

        List<VmSchedulingRuleRefVO> refVOS = SQL.New("select ref from VmSchedulingRuleRefVO ref, VmSchedulingRuleVO rule " +
                "where rule.uuid = ref.vmSchedulingRuleUuid and rule.state =:state " +
                "and ref.vmGroupUuid =:vmGroupUuid and rule.mode=:mode")
                .param("mode", VMSchedulingRuleMode.SOFT)
                .param("vmGroupUuid", vmGroupUuid)
                .param("state", AffinityGroupState.Enabled)
                .list();

        for (VmSchedulingRuleRefVO refVO : refVOS) {
            if (StringUtils.isEmpty(refVO.getHostGroupUuid())) {
                affinityGroupSort(refVO.getVmSchedulingRuleUuid());
                continue;
            }
            vmSchedulingRuleSort(refVO.getVmSchedulingRuleUuid());
        }
    }

    private void vmSchedulingRuleSort(String vmSchedulingRuleUuid) {
        VmSchedulingRuleVO vo = dbf.findByUuid(vmSchedulingRuleUuid, VmSchedulingRuleVO.class);

        if (vo == null) {
            logger.debug(String.format("vm scheduling rule group [uuid: %s] for vm instance [uuid: %s] not found", vmSchedulingRuleUuid, spec.getVmInstance().getUuid()));
            return;
        }

        List<String> hostUuids = SQL.New("select ref.hostUuid from HostSchedulingRuleGroupRefVO ref, VmSchedulingRuleRefVO ruleRef " +
                "where ruleRef.vmSchedulingRuleUuid =:ruleUuid and ruleRef.hostGroupUuid = ref.hostGroupUuid")
                .param("ruleUuid", vo.getUuid())
                .list();

        if (hostUuids.isEmpty()) {
            subCandidates.clear();
            subCandidates.addAll(candidates);
            logger.debug(String.format("sorted by vm scheduling rule [uuid: %s] the hosts %s", vmSchedulingRuleUuid,
                    subCandidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
            return;
        }

        final List<String> tempUuids = new ArrayList<>(hostUuids);

        if (vo.getRule() == VMSchedulingRuleType.AFFINITY) {
            List<HostInventory> tempHosts = candidates.stream()
                    .filter(hostInv -> tempUuids.contains(hostInv.getUuid()))
                    .collect(Collectors.toList());
            subCandidates.clear();
            subCandidates.addAll(tempHosts);
            logger.debug(String.format("sorted by vm scheduling rule [uuid: %s] the hosts %s",
                    vmSchedulingRuleUuid, subCandidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
            return;
        }

        List<HostInventory> tempHosts = candidates.stream()
                .filter(hostInv -> !tempUuids.contains(hostInv.getUuid()))
                .collect(Collectors.toList());
        subCandidates.clear();
        subCandidates.addAll(tempHosts);
        logger.debug(String.format("sorted by vm scheduling rule [uuid: %s] the hosts %s",
                vmSchedulingRuleUuid, subCandidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
    }

    private void affinityGroupSort(String agUuid) {
        AffinityGroupRatingFactory rating = filterFlow.getRating(spec, agUuid);
        if (rating == null){
            subCandidates.clear();
            subCandidates.addAll(candidates);
            return;
        }

        AffinityGroupRatingFactory.AffinityGroupRatingStruct struct = new AffinityGroupRatingFactory.AffinityGroupRatingStruct();
        struct.setSpec(spec);
        struct.setCandidates(candidates);
        struct.setAffinityGroupUuid(agUuid);
        Map<String, Long> hostRating = rating.ratingHostCandidates(struct);

        List<String> hostUuids = new ArrayList<>();
        AffinityGroupInventory inv = AffinityGroupInventory.valueOf(dbf.findByUuid(agUuid, AffinityGroupVO.class));
        if (inv.getPolicy().toString().equals(AffinityGroupPolicy.ANTISOFT.toString())){
            hostUuids = hostRating.entrySet().stream().filter(host -> host.getValue() == 0).map(host -> host.getKey()).collect(Collectors.toList());
        } else if (inv.getPolicy().toString().equals(AffinityGroupPolicy.AFFINITYSOFT.toString())){
            hostUuids = hostRating.entrySet().stream().filter(host -> host.getValue() > 0).map(host -> host.getKey()).collect(Collectors.toList());
        }

        if (!hostUuids.isEmpty()) {
            final List<String> tempUuids = new ArrayList<>(hostUuids);
            List<HostInventory> tempHosts = candidates.stream().filter(hostInv -> tempUuids.contains(hostInv.getUuid())).collect(Collectors.toList());
            subCandidates.clear();
            subCandidates.addAll(tempHosts);
            logger.debug(String.format("sorted by affinity group [uuid: %s] the hosts %s", agUuid, subCandidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
            return;
        }

        subCandidates.clear();
        subCandidates.addAll(candidates);
        logger.debug(String.format("sorted by affinity group [uuid: %s] the hosts %s", agUuid, subCandidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
        return;

    }


    @Override
    public boolean skipNext() {
        return false;
    }
}
