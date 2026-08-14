package org.zstack.compute.vmscheduling;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.affinityGroup.AffinityGroupFilterFlow;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.affinitygroup.AffinityGroupState;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.host.HostVO;
import org.zstack.header.vmscheduling.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * @Author: DaoDao
 * @Date: 2022/11/29
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSchedulingRuleFilterFlow implements HostAllocatorFilterExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmSchedulingRuleFilterFlow.class);
    @Autowired
    protected AffinityGroupFilterFlow affinityGroupFilterFlow;
    @Autowired
    private DatabaseFacade dbf;

    private String getVmGroupUuidFromAffinityGroup(HostAllocatorSpec spec) {
        String agUuid = affinityGroupFilterFlow.getAffinityGroupUuid(spec);

        if (StringUtils.isEmpty(agUuid)) {
            return null;
        }

        if (spec.getVmInstance() == null || Platform.FAKE_UUID.equals(spec.getVmInstance().getUuid())) {
            return Q.New(VmSchedulingRuleRefVO.class)
                    .eq(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, agUuid)
                    .select(VmSchedulingRuleRefVO_.vmGroupUuid)
                    .findValue();
        }

        return SQL.New("select ref.vmGroupUuid from VmSchedulingRuleRefVO ref, VmSchedulingRuleGroupRefVO vmGroupRef " +
                "where ref.vmGroupUuid = vmGroupRef.vmGroupUuid and ref.vmSchedulingRuleUuid = :vmSchedulingRuleUuid " +
                "and vmGroupRef.vmUuid = :vmUuid")
                .param("vmSchedulingRuleUuid", agUuid)
                .param("vmUuid", spec.getVmInstance().getUuid())
                .find();
    }

    public String getVmSchedulingRuleGroupUuid(HostAllocatorSpec spec) {
        String vmGroupUuid = getVmGroupUuidFromAffinityGroup(spec);
        if (!StringUtils.isEmpty(vmGroupUuid)) {
            return vmGroupUuid;
        }

        VmSchedulingRuleGroupRefVO vmGroup = Q.New(VmSchedulingRuleGroupRefVO.class)
                .eq(VmSchedulingRuleGroupRefVO_.vmUuid, spec.getVmInstance().getUuid())
                .find();

        if (vmGroup == null) {
            logger.debug(String.format("vm[uuid:%s] is not attached to any vm scheduling group", spec.getVmInstance().getUuid()));
            return null;
        }

        return vmGroup.getVmGroupUuid();
    }

    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec, String vmGroupUuid) {
        List<VmSchedulingRuleRefVO> refVOS = SQL.New("select ref from VmSchedulingRuleRefVO ref, VmSchedulingRuleVO rule " +
                "where rule.uuid = ref.vmSchedulingRuleUuid and rule.state =:state " +
                "and ref.vmGroupUuid =:vmGroupUuid and rule.mode=:mode")
                .param("mode", VMSchedulingRuleMode.HARD)
                .param("vmGroupUuid", vmGroupUuid)
                .param("state", AffinityGroupState.Enabled)
                .list();

        for (VmSchedulingRuleRefVO refVO : refVOS) {
            if (StringUtils.isEmpty(refVO.getHostGroupUuid())) {
                candidates = affinityGroupFilterFlow.filterHostCandidates(candidates, spec, refVO.getVmSchedulingRuleUuid());
                continue;
            }

            candidates = VmSchedulingRulefilterHostCandidates(candidates, spec, refVO.getVmSchedulingRuleUuid());
            logger.debug(String.format("vm scheduling rule filter host group candidates -> %s", candidates.stream().map(HostVO::getUuid).collect(Collectors.toList())));
        }

        return candidates;
    }


    public List<HostVO> VmSchedulingRulefilterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec, String vmSchedulingRuleUuid) {
        VmSchedulingRuleVO ruleVO = dbf.findByUuid(vmSchedulingRuleUuid, VmSchedulingRuleVO.class);

        if (ruleVO == null) {
            logger.debug(String.format("vm scheduling rule group [uuid: %s] for vm[uuid: %s] not found", vmSchedulingRuleUuid, spec.getVmInstance().getUuid()));
            return null;
        }

        List<String> hostUuids = SQL.New("select host.uuid from HostSchedulingRuleGroupRefVO ref, VmSchedulingRuleRefVO ruleRef, HostVO host " +
                "where ruleRef.vmSchedulingRuleUuid =:ruleUuid and ruleRef.hostGroupUuid = ref.hostGroupUuid " +
                "and host.uuid = ref.hostUuid")
                .param("ruleUuid", ruleVO.getUuid())
                .list();
        logger.debug(String.format("rule type : %s ,hostGroup -> %s, candidates -> %s",
                ruleVO.getRule(), hostUuids,
                candidates.stream().map(HostVO::getUuid).collect(Collectors.toList())));
        if (ruleVO.getRule() == VMSchedulingRuleType.ANTIAFFINITY) {
            return candidates.stream().filter(host -> !hostUuids.contains(host.getUuid())).collect(Collectors.toList());
        }
        return  candidates.stream().filter(host -> hostUuids.contains(host.getUuid())).collect(Collectors.toList());
    }

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        String vmGroupUuid = getVmSchedulingRuleGroupUuid(spec);
        if (vmGroupUuid == null) {
            return;
        }

        List<HostVO> hosts = transform(candidates, candidate -> candidate.host);
        hosts = filterHostCandidates(hosts, spec, vmGroupUuid);
        for (HostCandidate candidate : candidates) {
            if (!hosts.contains(candidate.host)) {
                candidate.markAsRejected(getClass(), i18m("can not satisfied vm scheduling rule group conditions"));
            }
        }
    }
}
