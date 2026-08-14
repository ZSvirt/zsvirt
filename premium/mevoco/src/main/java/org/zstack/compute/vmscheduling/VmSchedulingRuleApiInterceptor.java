package org.zstack.compute.vmscheduling;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.affinitygroup.AffinityGroupStateEvent;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vmscheduling.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.list;

/**
 * @Author: DaoDao
 * @Date: 2022/11/29
 */
@InterceptorForService("vmSchedulingRule")
public class VmSchedulingRuleApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private final static CLogger logger = Utils.getLogger(VmSchedulingRuleApiInterceptor.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmSchedulingRuleMsg) {
            validate((APICreateVmSchedulingRuleMsg) msg);
        } else if (msg instanceof APIChangeVmSchedulingRuleStateMsg) {
            validate((APIChangeVmSchedulingRuleStateMsg) msg);
        } else if (msg instanceof APIAddVmToVmSchedulingRuleGroupMsg) {
            validate((APIAddVmToVmSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIDetachVmFromVmSchedulingRuleGroupMsg) {
            validate((APIDetachVmFromVmSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIAddHostToHostSchedulingRuleGroupMsg) {
            validate((APIAddHostToHostSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIValidateVmSchedulingRuleMsg) {
            validate((APIValidateVmSchedulingRuleMsg) msg);
        } else if (msg instanceof APIUpdateVmSchedulingRuleMsg) {
            validate((APIUpdateVmSchedulingRuleMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIUpdateVmSchedulingRuleMsg msg) {
        if (!StringUtils.isEmpty(msg.getMode()) && msg.getMode().equals(VMSchedulingRuleMode.HARD.toString())) {
            VmSchedulingRuleVO vo = dbf.findByUuid(msg.getUuid(), VmSchedulingRuleVO.class);
            VmSchedulingRuleRefVO refVO = Q.New(VmSchedulingRuleRefVO.class)
                    .eq(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, vo.getUuid())
                    .find();

            if (VMSchedulingRuleType.AFFINITY.equals(vo.getRule())) {
                validateAffinity(refVO.getVmGroupUuid(), refVO.getHostGroupUuid(), vo.getUuid(),
                        vo.getRule().toString(), msg.getMode());
            } else if (VMSchedulingRuleType.ANTIAFFINITY.equals(vo.getRule())) {
                validateAntiAffinity(refVO.getVmGroupUuid(), refVO.getHostGroupUuid(), vo.getUuid(),
                        vo.getRule().toString(), msg.getMode());
            }
        }
    }

    private void validate(APIValidateVmSchedulingRuleMsg msg) {
        if (msg.getRule().equals(VMSchedulingRuleType.AFFINITY.toString())) {
            validateAffinity(msg.getVmGroupUuid(), msg.getHostGroupUuid(), null, msg.getRule(), msg.getMode());
        } else if (msg.getRule().equals(VMSchedulingRuleType.ANTIAFFINITY.toString())) {
            validateAntiAffinity(msg.getVmGroupUuid(), msg.getHostGroupUuid(), null, msg.getRule(), msg.getMode());
        }
    }

    private void validate(APIAddHostToHostSchedulingRuleGroupMsg msg) {
        HostSchedulingRuleGroupRefVO refVO = Q.New(HostSchedulingRuleGroupRefVO.class)
                .eq(HostSchedulingRuleGroupRefVO_.hostUuid, msg.getHostUuid()).find();

        if (refVO != null ) {
            throw new ApiMessageInterceptionException(argerr("the host[uuid:%s] already attached to host scheduling group[uuid:%s]",
                    msg.getHostUuid(), refVO.getHostGroupUuid()));
        }

        HostVO hostVO = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .find();

        if (StringUtils.isEmpty(hostVO.getClusterUuid())) {
            throw new ApiMessageInterceptionException(argerr("host clusterUuid is null"));
        }

        if (hostVO.getState() != HostState.Enabled || hostVO.getStatus() != HostStatus.Connected) {
            throw new ApiMessageInterceptionException(argerr("hosts that you can add to a host scheduling group must be enabled and connected to the MN."));
        }

        HostSchedulingRuleGroupVO hostGroup = dbf.findByUuid(msg.getHostGroupUuid(), HostSchedulingRuleGroupVO.class);

        if (!hostVO.getZoneUuid().equals(hostGroup.getZoneUuid())) {
            throw new ApiMessageInterceptionException(argerr("unmatched zone detected, host[uuid: %s, zone uuid: %s]'s zone is " +
                            "different from host sheduling rule group[uuid: %s, zone uuid: %s]", hostVO.getUuid(),
                    hostVO.getZoneUuid(), hostGroup.getUuid(), hostGroup.getZoneUuid()));
        }
    }

    private void validate(APIDetachVmFromVmSchedulingRuleGroupMsg msg) {
        validateVmState(msg.getVmUuid());
        validateVmSchedulingRuleGroup(msg.getVmGroupUuid());
    }

    private void validate(APIAddVmToVmSchedulingRuleGroupMsg msg) {
        VmSchedulingRuleGroupRefVO refVO = Q.New(VmSchedulingRuleGroupRefVO.class)
                .eq(VmSchedulingRuleGroupRefVO_.vmUuid, msg.getVmUuid()).find();
        if (refVO != null) {
            throw new ApiMessageInterceptionException(argerr("vm[uuid:%s] already attached to vm scheduling group[uuid:%s]",
                    msg.getVmUuid(), refVO.getVmGroupUuid()));
        }

        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmUuid())
                .find();

        VmSchedulingRuleGroupVO groupVO = Q.New(VmSchedulingRuleGroupVO.class)
                .eq(VmSchedulingRuleGroupVO_.uuid, msg.getVmSchedulingRuleGroupUuid())
                .find();

        if (!groupVO.getZoneUuid().equals(vm.getZoneUuid())) {
            throw new ApiMessageInterceptionException(argerr("unmatched zone detected, vm[uuid: %s, zone uuid: %s]'s zone is " +
                            "different from vm sheduling rule group[uuid: %s, zone uuid: %s]",
                    vm.getUuid(), vm.getZoneUuid(), groupVO.getUuid(), groupVO.getZoneUuid()));
        }

        validateVmState(vm.getUuid());
        validateVmSchedulingRuleGroup(msg.getVmGroupUuid());

    }

    private void validateVmState(String vmUuid) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .find();

        if (!(vm.getState().toString().equals(VmInstanceState.Running.toString()) || vm.getState().toString().equals(VmInstanceState.Stopped.toString()))) {
            throw new ApiMessageInterceptionException(argerr("vm can change its vm scheduling group only in state [%s,%s], but vm is in state [%s]",
                    VmInstanceState.Running.toString(), VmInstanceState.Stopped.toString(), vm.getState().toString()));
        }
    }

    private void validateVmSchedulingRuleGroup(String vmGroupUuid) {
        VmSchedulingRuleGroupVO groupVO = Q.New(VmSchedulingRuleGroupVO.class)
                .eq(VmSchedulingRuleGroupVO_.uuid, vmGroupUuid)
                .find();
        if (!groupVO.getAppliance().equals(VmSchedulingRuleConstants.VM_SCHEDULING_RULE_GROUP_APPLIANCE)) {
            throw new ApiMessageInterceptionException(argerr("cannot operate vpc vm scheduling group"));
        }
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VmSchedulingRuleGroupMessage) {
            VmSchedulingRuleGroupMessage groupMessage = (VmSchedulingRuleGroupMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmSchedulingRuleConstants.SERVICE_ID, groupMessage.getVmSchedulingRuleGroupUuid());
        } else if (msg instanceof VmSchedulingRuleMessage) {
            VmSchedulingRuleMessage ruleMessage = (VmSchedulingRuleMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmSchedulingRuleConstants.SERVICE_ID, ruleMessage.getVmSchedulingRuleUuid());
        } else if (msg instanceof HostSchedulingRuleGroupMessage) {
            HostSchedulingRuleGroupMessage groupMessage = (HostSchedulingRuleGroupMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmSchedulingRuleConstants.SERVICE_ID, groupMessage.getHostGroupUuid());
        }
    }

    private void validate(APIChangeVmSchedulingRuleStateMsg msg) {
        if (msg.getState().equals(AffinityGroupStateEvent.disable.toString())) {
            return;
        }

        VmSchedulingRuleVO vo = dbf.findByUuid(msg.getUuid(), VmSchedulingRuleVO.class);
        VmSchedulingRuleRefVO refVO = Q.New(VmSchedulingRuleRefVO.class)
                .eq(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, vo.getUuid())
                .find();

        if (VMSchedulingRuleType.AFFINITY.equals(vo.getRule())) {
            validateAffinity(refVO.getVmGroupUuid(), refVO.getHostGroupUuid(), vo.getUuid(),
                    vo.getRule().toString(), vo.getMode().toString());
        } else if (VMSchedulingRuleType.ANTIAFFINITY.equals(vo.getRule())) {
            validateAntiAffinity(refVO.getVmGroupUuid(), refVO.getHostGroupUuid(), vo.getUuid(),
                    vo.getRule().toString(), vo.getMode().toString());
        }
    }

    private void validate(APICreateVmSchedulingRuleMsg msg) {
        if (StringUtils.isEmpty(msg.getZoneUuid())) {
            throw new ApiMessageInterceptionException(argerr("zoneUuid is not null"));
        }

        if (msg.getRule().equals(VMSchedulingRuleType.AFFINITY.toString())) {
            validateAffinity(msg.getVmGroupUuid(), msg.getHostGroupUuid(), null, msg.getRule(), msg.getMode());
        } else {
            validateAntiAffinity(msg.getVmGroupUuid(), msg.getHostGroupUuid(), null, msg.getRule(), msg.getMode());
        }
        msg.setPolicy(VmSchedulingRulePolicy.getAffinityGroupPolicy(msg.getRule(), msg.getMode()).toString());
    }


    private void validateAntiAffinityVmGroup(String vmGroupUuid, String excludeRuleUuid, String mode) {
        List<VmSchedulingRuleVO> ruleVOList = SQL.New("select rule from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.vmGroupUuid= :groupUuid and " +
                "ref.hostGroupUuid is not null")
                .param("groupUuid", vmGroupUuid)
                .list();

        if (ruleVOList.isEmpty()) {
            return;
        }

        List<VmSchedulingRuleVO> ruleVOS = ruleVOList.stream()
                .filter(rule -> !rule.getUuid().equals(excludeRuleUuid)).collect(Collectors.toList());


        List<VmSchedulingRuleVO> affinityRules = ruleVOS.stream()
                .filter(rule -> rule.getRule() == VMSchedulingRuleType.AFFINITY)
                .collect(Collectors.toList());

        if (!affinityRules.isEmpty() && !StringUtils.isEmpty(mode) && !mode.equals(VMSchedulingRuleMode.SOFT.toString())) {
            List<String> affinityRuleUuids = affinityRules.stream()
                    .map(VmSchedulingRuleVO::getUuid)
                    .collect(Collectors.toList());

            List<String> hostGroupUuids = Q.New(VmSchedulingRuleRefVO.class)
                    .select(VmSchedulingRuleRefVO_.hostGroupUuid)
                    .in(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, affinityRuleUuids)
                    .listValues();

            long hostCount = Q.New(HostSchedulingRuleGroupRefVO.class)
                    .in(VmSchedulingRuleRefVO_.hostGroupUuid, hostGroupUuids)
                    .count();

            long vmCount = Q.New(VmSchedulingRuleGroupRefVO.class)
                    .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, vmGroupUuid)
                    .count();

            if (vmCount > hostCount) {
                throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VM_ANTIAFFINITY_TO_VMHOSTGROUP_HAS_AFFINITY,
                        "the vm scheduling group has already had a vms Affinitive to Hosts scheduling policy attached"));
            }
        }

        List<VmSchedulingRuleVO> antiAffinityRules = ruleVOS.stream()
                .filter(rule -> rule.getRule() == VMSchedulingRuleType.ANTIAFFINITY)
                .collect(Collectors.toList());

        if (!antiAffinityRules.isEmpty() && !StringUtils.isEmpty(mode) && !mode.equals(VMSchedulingRuleMode.SOFT.toString())) {
            long vmCount = Q.New(VmSchedulingRuleGroupRefVO.class)
                    .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, vmGroupUuid)
                    .count();

            List<String> antiAffinityRuleUuids = ruleVOS.stream().map(VmSchedulingRuleVO::getUuid).collect(Collectors.toList());

            List<String> hostGroupUuids = Q.New(VmSchedulingRuleRefVO.class)
                    .select(VmSchedulingRuleRefVO_.hostGroupUuid)
                    .in(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, antiAffinityRuleUuids)
                    .listValues();

            long previousHostCount = Q.New(HostSchedulingRuleGroupRefVO.class)
                    .in(VmSchedulingRuleRefVO_.hostGroupUuid, hostGroupUuids)
                    .count();
            String zoneUuid = antiAffinityRules.get(0).getZoneUuid();

            long totalHostCount = Q.New(HostVO.class).eq(HostVO_.zoneUuid, zoneUuid).count();

            if (vmCount >  totalHostCount - previousHostCount) {
                throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VM_ANTIAFFINITY_TO_VMHOSTGROUP_HAS_ANTIAFFINITY,
                        "the vm scheduling group has already had a vms antiaffinity from hosts scheduling rule attached. " +
                                "the number of hosts available for the vm in the scheduling group to run is less than that of the vm in the group. " +
                                "you cannot attach a vm antiaffinity from Each Other scheduling rule to the group"));
            }
        }
    }

    private void validateAntiAffinityVmGroupHostGroup(String vmGroupUuid, String hostGroupUuid, String excludeRuleUuid,
                                                      String ruleType, String mode) {
        validateVmGroupHostGroup(vmGroupUuid, hostGroupUuid, excludeRuleUuid, ruleType);

        List<VmSchedulingRuleVO> antiAffinityRules = SQL.New("select rule from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.vmGroupUuid= :groupUuid and " +
                "ref.hostGroupUuid is null and rule.rule =:ruleType")
                .param("groupUuid",  vmGroupUuid)
                .param("ruleType", VMSchedulingRuleType.ANTIAFFINITY)
                .list();

        if (!antiAffinityRules.isEmpty() && !StringUtils.isEmpty(mode) && !mode.equals(VMSchedulingRuleMode.SOFT.toString())) {
            long vmCount = Q.New(VmSchedulingRuleGroupRefVO.class)
                    .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, vmGroupUuid)
                    .count();

            long currentHostCount = Q.New(HostSchedulingRuleGroupRefVO.class)
                    .eq(HostSchedulingRuleGroupRefVO_.hostGroupUuid, hostGroupUuid)
                    .count();

            List<String> antiAffinityRuleUuids = antiAffinityRules.stream().map(VmSchedulingRuleVO::getUuid).collect(Collectors.toList());

            if (antiAffinityRuleUuids.isEmpty()) {
                return;
            }

            List<String> hostGroupUuids = Q.New(VmSchedulingRuleRefVO.class)
                    .select(VmSchedulingRuleRefVO_.hostGroupUuid)
                    .in(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, antiAffinityRuleUuids)
                    .listValues();

            long previousHostCount = Q.New(HostSchedulingRuleGroupRefVO.class)
                    .in(VmSchedulingRuleRefVO_.hostGroupUuid, hostGroupUuids)
                    .count();

            String zoneUuid = antiAffinityRules.get(0).getZoneUuid();

            long totalHostCount = Q.New(HostVO.class).eq(HostVO_.zoneUuid, zoneUuid).count();

            if (vmCount >  totalHostCount - currentHostCount - previousHostCount) {
                throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VMHOST_ANTIAFFINITY_TO_VMGROUP_HAS_ANTIAFFINITY,
                        "the vm scheduling group has already had a vm antiaffinity from each other scheduling rule attached." +
                                " the number of hosts available for the vm in the scheduling group to run is less than that of the vm in the group. " +
                                "you cannot attach a vms antiaffinity from Hosts scheduling policy to the group."));
            }
        }
    }


    private void validateAntiAffinity(String vmGroupUuid, String hostGroupUuid, String excludeRuleUuid, String rule, String mode) {
        if (hostGroupUuid == null) {
            validateVmGroup(vmGroupUuid, excludeRuleUuid, rule);
            validateAntiAffinityVmGroup(vmGroupUuid, excludeRuleUuid, mode);
            return;
        }
        validateAntiAffinityVmGroupHostGroup(vmGroupUuid, hostGroupUuid, excludeRuleUuid, rule, mode);
    }

    private void validateVmGroup(String vmGroupUuid, String excludeRuleUuid, String ruleType) {
        List<VmSchedulingRuleVO> ruleVOList = SQL.New("select rule from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.vmGroupUuid= :groupUuid and " +
                "ref.hostGroupUuid is null")
                .param("groupUuid", vmGroupUuid)
                .list();

        if (ruleVOList.isEmpty()) {
            return;
        }

        List<VmSchedulingRuleVO> ruleVOS = ruleVOList.stream()
                .filter(rule -> !rule.getUuid().equals(excludeRuleUuid)).collect(Collectors.toList());

        if (ruleVOS.isEmpty()) {
            return;
        }

        VmSchedulingRuleVO ruleVO = ruleVOS.get(0);
        if (ruleType.equals(VMSchedulingRuleType.ANTIAFFINITY.toString()) && ruleVO.getRule() == VMSchedulingRuleType.ANTIAFFINITY) {
            throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VM_ANTIAFFINITY_TO_VMGROUP_HAS_ANTIAFFINITY,
                    "the vm scheduling group[uuid:%s] has already had a vm antiaffinity from each other scheduling rule attached. " +
                            "attaching another one is not allowed.", vmGroupUuid));
        }

        if (ruleType.equals(VMSchedulingRuleType.ANTIAFFINITY.toString()) && ruleVO.getRule() == VMSchedulingRuleType.AFFINITY) {
            throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VM_ANTIAFFINITY_TO_VMGROUP_HAS_AFFINITY,
                    "the vm scheduling group[uuid:%s] has already had a vm affinitive to each other scheduling rule attached." +
                            " Attaching a vm antiaffinity from each other scheduling rule is not allowed.", vmGroupUuid));
        }

        if (ruleType.equals(VMSchedulingRuleType.AFFINITY.toString()) && ruleVO.getRule() == VMSchedulingRuleType.AFFINITY) {
            throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VM_AFFINITY_TO_VMGROUP_HAS_AFFINITY,
                    "the vm scheduling group[uuid:%s] has already had a vm affinitive to each other scheduling rule attached." +
                            "attaching another one is not allowed.", vmGroupUuid));
        }

        if (ruleType.equals(VMSchedulingRuleType.AFFINITY.toString()) && ruleVO.getRule() == VMSchedulingRuleType.ANTIAFFINITY) {
            throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VM_AFFINITY_TO_VMGROUP_HAS_ANTIAFFINITY,
                    "the vm scheduling group[uuid:%s] has already had a vm exclusive from each other scheduling rule attached. " +
                            "Attaching a vm affinitive to each other scheduling policy is not allowed.", vmGroupUuid));
        }

        throw new ApiMessageInterceptionException(argerr("the vm scheduling group[uuid:%s] has already had a executed exclusive vm or affinitive vm scheduling policy attached." +
                " you cannot attach either of the two scheduling policies that require execution to the group again"));
    }

    private void validateAffinity(String vmGroupUuid, String hostGroupUuid, String excludeRuleUuid, String rule, String mode) {
        if (StringUtils.isEmpty(hostGroupUuid)) {
            validateVmGroup(vmGroupUuid, excludeRuleUuid, rule);
            return;
        }
        validateAffinityVmGroupHostGroup(vmGroupUuid, hostGroupUuid, excludeRuleUuid, rule, mode);
    }

    private void validateVmGroupHostGroup(String vmGroupUuid, String hostGroupUuid, String excludeRuleUuid, String ruleType) {
        List<VmSchedulingRuleVO> ruleVOList = SQL.New("select rule from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.vmGroupUuid= :groupUuid and " +
                "ref.hostGroupUuid is not null")
                .param("groupUuid", vmGroupUuid)
                .list();

        if (ruleVOList.isEmpty()) {
            return;
        }

        List<VmSchedulingRuleVO> ruleVOS = ruleVOList.stream()
                .filter(rule -> !rule.getUuid().equals(excludeRuleUuid)).collect(Collectors.toList());

        if (ruleVOS.isEmpty()) {
            return;
        }

        if (ruleType.equals(VMSchedulingRuleType.AFFINITY.toString()) &&
                ruleVOS.stream().anyMatch(vo -> vo.getRule() == VMSchedulingRuleType.AFFINITY)) {
            throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VMHOST_AFFINITY_TO_VMHOSTGROUP_HAS_AFFINITY,
                    "the vm scheduling group[uuid:%s] has already had a vms affinitive to hosts scheduling rule attached. " +
                            "you cannot attach another one to the group again.", vmGroupUuid));
        }

        if (ruleType.equals(VMSchedulingRuleType.AFFINITY.toString()) &&
                ruleVOS.stream().anyMatch(vo -> vo.getRule() == VMSchedulingRuleType.ANTIAFFINITY)) {
            throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VMHOST_AFFINITY_TO_VMHOSTGROUP_HAS_ANTIAFFINITY,
                    "the vm scheduling group[uuid:%s] has already had a vm antiaffinity from host scheduling rule attached." +
                            " you cannot attach a vms affinitive to host scheduling rule to the group.", vmGroupUuid));
        }

        if (ruleType.equals(VMSchedulingRuleType.ANTIAFFINITY.toString()) &&
                ruleVOS.stream().anyMatch(vo -> vo.getRule() == VMSchedulingRuleType.AFFINITY)) {
            throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VMHOST_ANTIAFFINITY_TO_VMHOSTGROUP_HAS_AFFINITY,
                    "the vm scheduling group[uuid:%s] has already had a vm affinitive to hosts scheduling rule attached. " +
                            "you cannot attach a vm antiaffinity from hosts scheduling rule to the group.", vmGroupUuid));
        }
    }

    private void validateAffinityVmGroupHostGroup(String vmGroupUuid, String hostGroupUuid, String excludeRuleUuid,
                                                  String ruleType, String mode) {
        validateVmGroupHostGroup(vmGroupUuid, hostGroupUuid, excludeRuleUuid, ruleType);

        if (!StringUtils.isEmpty(mode) && mode.equals(VMSchedulingRuleMode.SOFT.toString())) {
            return;
        }

        if (SQL.New("select rule from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.vmGroupUuid= :groupUuid and " +
                "rule.rule = :ruleType  and " +
                "ref.hostGroupUuid is null")
                .param("groupUuid", vmGroupUuid)
                .param("ruleType", VMSchedulingRuleType.ANTIAFFINITY)
                .list().size() > 0) {
            long vmCount = Q.New(VmSchedulingRuleGroupRefVO.class)
                    .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, vmGroupUuid)
                    .count();

            long hostCount = Q.New(HostSchedulingRuleGroupRefVO.class)
                    .eq(HostSchedulingRuleGroupRefVO_.hostGroupUuid, hostGroupUuid)
                    .count();

            if (vmCount > hostCount) {
                throw new ApiMessageInterceptionException(err(VmSchedulingRuleErrors.ADD_VMHOST_AFFINITY_TO_VMGROUP_HAS_ANTIAFFINITY,
                        "the vm scheduling group has already had a vm antiaffinity from each other scheduling rule attached. " +
                                "the number of hosts available for the vm in the scheduling group to run is less than that of the vm in the group. " +
                                "you cannot attach a vms affinitive to hosts scheduling policy to the group."));
            }
        }
    }


    @Override
    public List<Class> getMessageClassToIntercept() {
        return list(
            APICreateVmSchedulingRuleMsg.class,
            APIRemoveVmSchedulingRuleMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
