package org.zstack.compute.vmscheduling;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.affinityGroup.AffinityGroupManager;
import org.zstack.compute.affinityGroup.DeleteAffinityGroupExtensionPoint;
import org.zstack.compute.affinityGroup.VmRelationAffinityGroupExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.AbstractService;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vm.VmInstanceBeforeStartExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vmscheduling.*;
import org.zstack.identity.AccountManager;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.*;

public class VmSchedulingRuleManagerImpl extends AbstractService implements DeleteAffinityGroupExtensionPoint,
        VmInstanceBeforeStartExtensionPoint, VmRelationAffinityGroupExtensionPoint, AffinityGroupSubTypeFactory {

    private final static CLogger logger = Utils.getLogger(VmSchedulingRuleManagerImpl.class);
    public static AffinityGroupSubType affinityGroupSubType = new AffinityGroupSubType(VmSchedulingRuleConstants.VM_SCHEDULING_RULE_TYPE);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected AffinityGroupManager agMgr;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof VmSchedulingRuleMessage) {
            passThrough((VmSchedulingRuleMessage) msg);
        } else if (msg instanceof VmSchedulingRuleGroupMessage) {
            passThrough((VmSchedulingRuleGroupMessage) msg);
        } else if (msg instanceof HostSchedulingRuleGroupMessage) {
            passThrough((HostSchedulingRuleGroupMessage) msg);
        } else if(msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(HostSchedulingRuleGroupMessage msg) {
        HostSchedulingRuleGroupVO vo = Q.New(HostSchedulingRuleGroupVO.class)
                .eq(VmSchedulingRuleVO_.uuid, msg.getHostGroupUuid())
                .find();
        if (vo == null) {
            throw new OperationFailureException(argerr("cannot find the host scheduling group[uuid:%s], " +
                    "it may have been deleted", msg.getHostGroupUuid()));
        }

        HostSchedulingRuleGroupBase ag = new HostSchedulingRuleGroupBase(vo);
        ag.handleMessage((Message) msg);
    }

    private void passThrough(VmSchedulingRuleMessage msg) {
        VmSchedulingRuleVO vo = Q.New(VmSchedulingRuleVO.class)
                .eq(VmSchedulingRuleVO_.uuid, msg.getVmSchedulingRuleUuid())
                .find();
        if (vo == null) {
            throw new OperationFailureException(argerr("cannot find the vm scheduling rule[uuid:%s], " +
                    "it may have been deleted", msg.getVmSchedulingRuleUuid()));
        }

        VmSchedulingRuleBase ag = new VmSchedulingRuleBase(vo);
        ag.handleMessage((Message) msg);
    }

    private void passThrough(VmSchedulingRuleGroupMessage msg) {
        VmSchedulingRuleGroupVO vo = Q.New(VmSchedulingRuleGroupVO.class)
                .eq(VmSchedulingRuleVO_.uuid, msg.getVmSchedulingRuleGroupUuid())
                .find();
        if (vo == null) {
            throw new OperationFailureException(argerr("cannot find the vm scheduling group[uuid:%s], " +
                    "it may have been deleted", msg.getVmSchedulingRuleGroupUuid()));
        }

        VmSchedulingRuleGroupBase group = new VmSchedulingRuleGroupBase(vo);
        group.handleMessage((Message) msg);
    }


    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVmSchedulingRuleGroupMsg) {
            handle((APICreateVmSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APICreateHostSchedulingRuleGroupMsg) {
            handle((APICreateHostSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIGetVmSchedulingRulesExecuteStateMsg) {
            handle((APIGetVmSchedulingRulesExecuteStateMsg) msg);
        } else if (msg instanceof APIValidateVmSchedulingRuleMsg) {
            handle((APIValidateVmSchedulingRuleMsg) msg);
        } else if (msg instanceof APIListVmSchedulingRulesFromExecuteStateMsg) {
            handle((APIListVmSchedulingRulesFromExecuteStateMsg) msg);
        } else if (msg instanceof APIGetVmsSchedulingStateFromSchedulingRuleMsg) {
            handle((APIGetVmsSchedulingStateFromSchedulingRuleMsg) msg);
        } else if (msg instanceof APIListVmsFromSchedulingStateMsg) {
            handle((APIListVmsFromSchedulingStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIListVmsFromSchedulingStateMsg msg) {
        APIListVmsFromSchedulingStateReply reply = new APIListVmsFromSchedulingStateReply();
        List<String> conflictVmUuids = getConflictVmUuids(msg.getRuleUuid());
        List<String> vmUuids = SQL.New("select groupRef.vmUuid from VmSchedulingRuleGroupRefVO groupRef, VmSchedulingRuleRefVO ruleRef" +
                " where groupRef.vmGroupUuid = ruleRef.vmGroupUuid and ruleRef.vmSchedulingRuleUuid =:ruleUuid")
                .param("ruleUuid", msg.getRuleUuid())
                .list();

        VmSchedulingRuleVO rule = dbf.findByUuid(msg.getRuleUuid(), VmSchedulingRuleVO.class);
        List<String> returnVmUuids = new ArrayList<>();
        for (String executeState : msg.getExecuteStates()) {
            if (executeState.equals(VmSchedulingRuleExecuteState.Invalid.toString())
                    && rule.getState() == AffinityGroupState.Disabled) {
                returnVmUuids.addAll(vmUuids);
            }

            if (executeState.equals(VmSchedulingRuleExecuteState.Conflict.toString())
                    && rule.getState() != AffinityGroupState.Disabled) {
                returnVmUuids.addAll(conflictVmUuids);
            }

            if (executeState.equals(VmSchedulingRuleExecuteState.Normal.toString())
                    && rule.getState() != AffinityGroupState.Disabled) {
                returnVmUuids.addAll(vmUuids.stream()
                        .filter(uuid -> !conflictVmUuids.contains(uuid))
                        .collect(Collectors.toList()));
            }
        }

        reply.setUuids(returnVmUuids);
        bus.reply(msg, reply);
    }

    private void handle(APIGetVmsSchedulingStateFromSchedulingRuleMsg msg) {
        APIGetVmsSchedulingStateFromSchedulingRuleReply reply = new APIGetVmsSchedulingStateFromSchedulingRuleReply();
        VmSchedulingRuleVO rule = dbf.findByUuid(msg.getRuleUuid(), VmSchedulingRuleVO.class);
        Map<String, VmSchedulingRuleExecuteState> ruleMapState = new HashMap<>();
        if (rule.getState() == AffinityGroupState.Disabled) {
            for (String uuid : msg.getVmUuids()) {
                ruleMapState.put(uuid, VmSchedulingRuleExecuteState.Invalid);
            }
            reply.setRuleMapState(ruleMapState);
            bus.reply(msg, reply);
            return;
        }

        VmSchedulingRuleRefVO refVO = Q.New(VmSchedulingRuleRefVO.class)
                .eq(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, rule.getUuid())
                .find();

        List<String> vmUuids = Q.New(VmSchedulingRuleGroupRefVO.class)
                .select(VmSchedulingRuleGroupRefVO_.vmUuid)
                .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, refVO.getVmGroupUuid())
                .listValues();

        List<String> conflictVmUuids = new ArrayList<>();
        if (StringUtils.isEmpty(refVO.getHostGroupUuid())) {
            conflictVmUuids.addAll(checkVmRuleConflict(rule, vmUuids));
        } else {
            conflictVmUuids.addAll(checkVmHostRuleConflict(rule, vmUuids, refVO.getHostGroupUuid()));
        }

        for (String vmUuid : msg.getVmUuids()) {
            ruleMapState.put(vmUuid, conflictVmUuids.contains(vmUuid) ? VmSchedulingRuleExecuteState.Conflict : VmSchedulingRuleExecuteState.Normal);
        }
        reply.setRuleMapState(ruleMapState);
        bus.reply(msg, reply);
    }

    private void handle(APIListVmSchedulingRulesFromExecuteStateMsg msg) {
        APIListVmSchedulingRulesFromExecuteStateReply reply = new APIListVmSchedulingRulesFromExecuteStateReply();
        List<String> uuids = new ArrayList<>();
        for (String state : msg.getExecuteStates()) {
            uuids.addAll(getVmSchedulingRuleUuidsFromExecuteState(state));
        }
        reply.setUuids(uuids);
        bus.reply(msg, reply);
    }

    private List<String> getVmSchedulingRuleNormalState() {
        List<String> uuids = new ArrayList<>();
        List<VmSchedulingRuleVO> vos = Q.New(VmSchedulingRuleVO.class)
                .notEq(VmSchedulingRuleVO_.state, AffinityGroupState.Disabled)
                .eq(VmSchedulingRuleVO_.appliance, AffinityGroupAppliance.CUSTOMER.toString())
                .list();

        for (VmSchedulingRuleVO vo : vos) {
            if (getConflictVmUuids(vo.getUuid()).isEmpty()) {
                uuids.add(vo.getUuid());
            }
        }
        return uuids;
    }

    private List<String> getVmSchedulingRuleConflictState() {
        List<String> uuids = new ArrayList<>();
        List<VmSchedulingRuleVO> vos = Q.New(VmSchedulingRuleVO.class)
                .notEq(VmSchedulingRuleVO_.state, AffinityGroupState.Disabled)
                .eq(VmSchedulingRuleVO_.appliance, AffinityGroupAppliance.CUSTOMER.toString())
                .list();

        for (VmSchedulingRuleVO vo : vos) {
            if (!getConflictVmUuids(vo.getUuid()).isEmpty()) {
                uuids.add(vo.getUuid());
            }
        }
        return uuids;
    }

    private List<String> getVmSchedulingRuleUuidsFromExecuteState(String executeState) {
        List<String> uuids = new ArrayList<>();
        switch (VmSchedulingRuleExecuteState.valueOf(executeState)) {
            case Invalid:
                uuids = Q.New(VmSchedulingRuleVO.class)
                        .eq(VmSchedulingRuleVO_.state, AffinityGroupState.Disabled)
                        .select(VmSchedulingRuleVO_.uuid)
                        .listValues();
                break;
            case Normal:
                uuids = getVmSchedulingRuleNormalState();
                break;
            case Conflict:
                uuids = getVmSchedulingRuleConflictState();
                break;
        }
        return uuids;
    }

    private void handle(APIValidateVmSchedulingRuleMsg msg) {
        APIValidateVmSchedulingRuleReply reply = new APIValidateVmSchedulingRuleReply();
        bus.reply(msg, reply);
    }

    private List<String> getConflictVmUuids(String vmSchedulingRuleUuid) {
        List<String> conflictVmUuids = new ArrayList<>();

        VmSchedulingRuleVO rule = dbf.findByUuid(vmSchedulingRuleUuid, VmSchedulingRuleVO.class);
        VmSchedulingRuleRefVO refVO = Q.New(VmSchedulingRuleRefVO.class)
                .eq(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, vmSchedulingRuleUuid)
                .find();
        if (refVO == null) {
            return conflictVmUuids;
        }

        List<String> vmUuids = Q.New(VmSchedulingRuleGroupRefVO.class)
                .select(VmSchedulingRuleGroupRefVO_.vmUuid)
                .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, refVO.getVmGroupUuid())
                .listValues();
        if (vmUuids.isEmpty()) {
            return conflictVmUuids;
        }

        if (StringUtils.isEmpty(refVO.getHostGroupUuid())) {
            conflictVmUuids.addAll(checkVmRuleConflict(rule, vmUuids));
        } else {
            conflictVmUuids.addAll(checkVmHostRuleConflict(rule, vmUuids, refVO.getHostGroupUuid()));
        }

        return conflictVmUuids;
    }

    private List<String> checkVmHostRuleConflict(VmSchedulingRuleVO vo, List<String> vmUuids, String hostGroupUuid) {
        if (vo.getState() == AffinityGroupState.Disabled) {
            return new ArrayList<>();
        }

        List<String> hostUuids = Q.New(HostSchedulingRuleGroupRefVO.class)
                .select(HostSchedulingRuleGroupRefVO_.hostUuid)
                .eq(HostSchedulingRuleGroupRefVO_.hostGroupUuid, hostGroupUuid)
                .listValues();

        if (CollectionUtils.isEmpty(hostUuids)) {
            return new ArrayList<>();
        }

        String sql = "select uuid from VmInstanceVO where uuid in (:uuids) and state =:state";
        if (VMSchedulingRuleType.AFFINITY == vo.getRule()) {
            sql += " and hostUuid not in (:hostUuids)";
        } else {
            sql += " and hostUuid in (:hostUuids)";
        }

        return SQL.New(sql)
                .param("uuids", vmUuids)
                .param("state", VmInstanceState.Running)
                .param("hostUuids", hostUuids)
                .list();
    }

    private List<String> checkVmRuleConflict(VmSchedulingRuleVO vo, List<String> vmUuids) {
        if (vo.getState() == AffinityGroupState.Disabled) {
            return new ArrayList<>();
        }

        if (VMSchedulingRuleType.AFFINITY == vo.getRule()) {
            List<String> hostUuids = SQL.New("select distinct hostUuid from VmInstanceVO " +
                    "where state =:state and uuid in (:uuids)  group by hostUuid order by count(hostUuid) desc", String.class)
                    .param("state", VmInstanceState.Running)
                    .param("uuids", vmUuids)
                    .list();
            if (hostUuids.isEmpty() || hostUuids.size() <= 1) {
                return new ArrayList<>();
            }

            return SQL.New("select uuid from VmInstanceVO " +
                    "where uuid in (:uuids) and state =:state and hostUuid in (:hostUuids)")
                    .param("uuids", vmUuids)
                    .param("state", VmInstanceState.Running)
                    .param("hostUuids", hostUuids.subList(1, hostUuids.size()))
                    .list();
        }

        return SQL.New("select uuid from VmInstanceVO where uuid in (:uuids) and " +
                "hostUuid in (select hostUuid from VmInstanceVO where state =:state and " +
                "uuid in (:uuids) group by hostUuid having count(hostUuid) > 1)")
                .param("state", VmInstanceState.Running)
                .param("uuids", vmUuids)
                .list();
    }

    private void handle(APIGetVmSchedulingRulesExecuteStateMsg msg) {
        APIGetVmSchedulingRulesExecuteStateReply reply = new APIGetVmSchedulingRulesExecuteStateReply();
        List<VmSchedulingRuleVO> vos = Q.New(VmSchedulingRuleVO.class)
                .in(VmSchedulingRuleVO_.uuid, msg.getUuids())
                .list();

        Map<String, VmSchedulingRuleExecuteState> ruleMap = new HashMap<>();
        for (VmSchedulingRuleVO vo : vos) {
            if (vo.getState() == AffinityGroupState.Disabled) {
                ruleMap.put(vo.getUuid(), VmSchedulingRuleExecuteState.Invalid);
                continue;
            }
            ruleMap.put(vo.getUuid(),
                    getConflictVmUuids(vo.getUuid()).isEmpty() ? VmSchedulingRuleExecuteState.Normal : VmSchedulingRuleExecuteState.Conflict);
        }
        reply.setRuleMapState(ruleMap);
        bus.reply(msg, reply);
    }

    private void handle(APICreateHostSchedulingRuleGroupMsg msg) {
        APICreateHostSchedulingRuleGroupEvent evt = new APICreateHostSchedulingRuleGroupEvent(msg.getId());
        HostSchedulingRuleGroupVO vo = new HostSchedulingRuleGroupVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setZoneUuid(msg.getZoneUuid());
        vo = dbf.persistAndRefresh(vo);
        evt.setInventory(HostSchedulingRuleGroupInventory.valueOf(vo));
        bus.publish(evt);
    }


    private void handle(APICreateVmSchedulingRuleGroupMsg msg) {
        APICreateVmSchedulingRuleGroupEvent evt = new APICreateVmSchedulingRuleGroupEvent(msg.getId());
        VmSchedulingRuleGroupVO vo = new VmSchedulingRuleGroupVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setAppliance(VmSchedulingRuleConstants.VM_SCHEDULING_RULE_GROUP_APPLIANCE);
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        evt.setInventory(VmSchedulingRuleGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmSchedulingRuleConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }


    @Override
    public AffinityGroupSubType getAffinityGroupSubType() {
        return affinityGroupSubType;
    }

    @Override
    public AffinityGroupVO persistAffinityGroup(CreateAffinityGroupMsg msg, AffinityGroupVO vo) {
        return createAffinityGroup(vo);
    }

    @Override
    public AffinityGroupVO persistAffinityGroup(APICreateAffinityGroupMsg msg, AffinityGroupVO vo) {
        if (msg instanceof APICreateVmSchedulingRuleMsg) {
            return createVmSchedulingRule((APICreateVmSchedulingRuleMsg)msg, vo);
        }
        return createAffinityGroup(vo);
    }

    private VmSchedulingRuleVO createVmSchedulingRule(APICreateVmSchedulingRuleMsg msg, AffinityGroupVO groupVO) {
        VmSchedulingRuleVO ruleVO = createVmSchedulingRule(groupVO);
        createVmSchedulingRuleRef(ruleVO.getUuid(), msg.getVmGroupUuid(), msg.getHostGroupUuid());
        vmGroupCreateAffinityGroupUsage(ruleVO.getUuid(), msg.getVmGroupUuid(), msg.getHostGroupUuid());
        return ruleVO;
    }

    private void vmGroupCreateAffinityGroupUsage(String vmSchedulingRuleUuid, String vmGroupUuid, String hostGroupUuid ) {
        if (!StringUtils.isEmpty(hostGroupUuid)) {
            return;
        }
        List<String> vmUuids = Q.New(VmSchedulingRuleGroupRefVO.class)
                .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, vmGroupUuid)
                .select(VmSchedulingRuleGroupRefVO_.vmUuid)
                .listValues();
        vmUuids.forEach(uuid -> agMgr.addVmToAffinityGroupUsage(vmSchedulingRuleUuid, uuid));

    }

    private VmSchedulingRuleVO createAffinityGroup(AffinityGroupVO groupVO) {
        VmSchedulingRuleVO ruleVO = createVmSchedulingRule(groupVO);
        VmSchedulingRuleGroupVO ruleGroupVO = createVmSchedulingRuleGroupSrcAffinityGroup(groupVO);
        createVmSchedulingRuleRef(ruleVO.getUuid(), ruleGroupVO.getUuid(), null);
        return ruleVO;
    }


    private VmSchedulingRuleVO createVmSchedulingRule(AffinityGroupVO vo) {
        VmSchedulingRuleVO ruleVO = new VmSchedulingRuleVO(vo);
        ruleVO.setRule(VmSchedulingRulePolicy.getVMSchedulingRuleType(vo.getPolicy()));
        ruleVO.setMode(VmSchedulingRulePolicy.getVMSchedulingRuleLevel(vo.getPolicy()));
        return dbf.persistAndRefresh(ruleVO);
    }

    private VmSchedulingRuleGroupVO createVmSchedulingRuleGroupSrcAffinityGroup(AffinityGroupVO vo) {
        VmSchedulingRuleGroupVO groupVO = new VmSchedulingRuleGroupVO();
        groupVO.setUuid(Platform.getUuid());
        groupVO.setName(String.format(VmSchedulingRuleConstants.CREATE_VM_GROUP_SRC_AFFINITYGROUP, vo.getName()));
        groupVO.setDescription(vo.getDescription());
        groupVO.setAppliance(vo.getAppliance());
        groupVO.setZoneUuid(vo.getZoneUuid());
        groupVO.setSrcUuid(vo.getUuid());
        groupVO.setAccountUuid(vo.getAccountUuid());
        return dbf.persistAndRefresh(groupVO);
    }

    private void createVmSchedulingRuleRef(String vmSchedulingRuleUuid, String vmGroupUuid, String hostGroupUuid) {
        VmSchedulingRuleRefVO refVO = new VmSchedulingRuleRefVO();
        refVO.setVmSchedulingRuleUuid(vmSchedulingRuleUuid);
        refVO.setVmGroupUuid(vmGroupUuid);
        refVO.setHostGroupUuid(hostGroupUuid);
        dbf.persist(refVO);
    }

    @Override
    public void beforeDeleteAffinityGroup(String affinityGroupUuid) {
        UpdateQuery.New(VmSchedulingRuleGroupVO.class)
                .eq(VmSchedulingRuleGroupVO_.srcUuid, affinityGroupUuid)
                .hardDelete();
    }

    @Override
    public void afterDeleteAffinityGroup(String affinityGroupUuid) {

    }

    private void createAffinityGroupSystemTags(String vmUuid, List<String> tags) {
        PatternedSystemTag tag =  AffinityGroupSystemTags.AFFINITY_GROUP_UUID;
        String token = AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN;

        String agUuid = SystemTagUtils.findTagValue(tags, tag, token);
        if(!StringUtils.isEmpty(agUuid)){
            String vmGroupUuid = Q.New(VmSchedulingRuleRefVO.class)
                    .eq(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, agUuid)
                    .select(VmSchedulingRuleRefVO_.vmGroupUuid)
                    .findValue();

            addVmToVmSchedulingRuleGroup(vmGroupUuid, vmUuid);
        }
    }

    @Override
    public ErrorCode handleSystemTag(String vmUuid, List<String> tags) {
        createAffinityGroupSystemTags(vmUuid, tags);
        
        PatternedSystemTag tag = VmSchedulingRuleSystemTags.VM_SCHEDULING_RULE_GROUP_UUID;
        String token = VmSchedulingRuleSystemTags.VM_SCHEDULING_RULE_GROUP_UUID_TOKEN;

        String vmGroupUuid = SystemTagUtils.findTagValue(tags, tag, token);
        if(StringUtils.isEmpty(vmGroupUuid)){
            return null;
        }

        createVmSystemTagForVmSchedulingRuleGroup(vmGroupUuid, vmUuid);
        addVmToVmSchedulingRuleGroup(vmGroupUuid, vmUuid);
        addVmToAffinity(vmGroupUuid, vmUuid);
        return null;
    }

    private void addVmToAffinity(String vmGroupUuid, String vmUuid) {
        List<VmSchedulingRuleRefVO> refVOS =  SQL.New("select ref from VmSchedulingRuleRefVO ref, VmSchedulingRuleVO rule " +
                "where ref.vmSchedulingRuleUuid = rule.uuid and ref.vmGroupUuid =:vmGroupUuid")
                .param("vmGroupUuid", vmGroupUuid)
                .list();

        refVOS.stream().filter(ref -> StringUtils.isEmpty(ref.getHostGroupUuid())).forEach(ref -> {
            agMgr.createVmSystemTagForAffinityGroup(ref.getVmSchedulingRuleUuid(), vmUuid);
            agMgr.addVmToAffinityGroupUsage(ref.getVmSchedulingRuleUuid(), vmUuid);
        });
    }


    private void createVmSystemTagForVmSchedulingRuleGroup(String vmGroupUuid, String vmUuid) {
        PatternedSystemTag tag = VmSchedulingRuleSystemTags.VM_SCHEDULING_RULE_GROUP_UUID;

        /* remove vm old nonherent systemTag  */
        if (tag.getTokenByResourceUuid(vmUuid, VmSchedulingRuleSystemTags.VM_SCHEDULING_RULE_GROUP_UUID_TOKEN) != null) {
            VmSchedulingRuleSystemTags.VM_SCHEDULING_RULE_GROUP_UUID.delete(vmUuid);
        }

        SystemTagCreator creator = VmSchedulingRuleSystemTags.VM_SCHEDULING_RULE_GROUP_UUID.newSystemTagCreator(vmUuid);
        creator.inherent = true;
        creator.recreate = true;
        creator.setTagByTokens(map(e(VmSchedulingRuleSystemTags.VM_SCHEDULING_RULE_GROUP_UUID_TOKEN, vmGroupUuid)));
        creator.create();
    }

    private void addVmToVmSchedulingRuleGroup(String vmGroupUuid, String vmUuid) {
        VmSchedulingRuleGroupRefVO refVO = new VmSchedulingRuleGroupRefVO();
        refVO.setVmUuid(vmUuid);
        refVO.setVmGroupUuid(vmGroupUuid);
        dbf.persist(refVO);
    }

    private void removeVmFromSchedulingRuleGroup(String vmGroupUuid, String vmUuid) {
        UpdateQuery.New(VmSchedulingRuleGroupRefVO.class)
                .eq(VmSchedulingRuleGroupRefVO_.vmUuid, vmUuid)
                .eq(VmSchedulingRuleGroupRefVO_.vmGroupUuid, vmGroupUuid)
                .hardDelete();
    }

    @Override
    public void afterAddVmToAffinityGroup(String agUuid, String vmUuid) {
        addVmToVmSchedulingRuleGroup(getVmGroupUuid(agUuid), vmUuid);
    }

    private String getVmGroupUuid(String agUuid) {
        return Q.New(VmSchedulingRuleRefVO.class)
                .eq(VmSchedulingRuleRefVO_.vmSchedulingRuleUuid, agUuid)
                .isNull(VmSchedulingRuleRefVO_.hostGroupUuid)
                .select(VmSchedulingRuleRefVO_.vmGroupUuid)
                .findValue();
    }

    @Override
    public void afterRemoveVmFromAffinityGroup(String agUuid, String vmUuid) {
        removeVmFromSchedulingRuleGroup(getVmGroupUuid(agUuid), vmUuid);
    }
}
