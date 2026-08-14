package org.zstack.compute.affinityGroup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.allocator.AttachVmInstanceToAffinityGroupExtensionPoint;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vm.*;
import org.zstack.header.vmscheduling.VmSchedulingRuleConstants;
import org.zstack.identity.AccountManager;
import org.zstack.identity.ResourceHelper;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.*;

public class AffinityGroupManagerImpl extends AbstractService implements AffinityGroupManager, AttachVmInstanceToAffinityGroupExtensionPoint,
        VmInstanceBeforeStartExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint, VmJustBeforeDeleteFromDbExtensionPoint,
        ReportQuotaExtensionPoint {
    private final static CLogger logger = Utils.getLogger(AffinityGroupManagerImpl.class);
    private Map<String, AffinityGroupRatingFactory> factories = Collections.synchronizedMap(new HashMap<String, AffinityGroupRatingFactory>());

    private Map<String, AffinityGroupSubTypeFactory> subTypeFactoryMap = new ConcurrentHashMap<>();

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
    private EventFacade evtf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    protected AffinityGroupFilterFlow filterFlow;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof AffinityGroupMessage) {
            passThrough((AffinityGroupMessage)msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else if (msg instanceof CreateAffinityGroupMsg) {
            handle((CreateAffinityGroupMsg)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAffinityGroupMsg) {
            handle((APICreateAffinityGroupMsg) msg);
        } else if (msg instanceof APIGetCandidateAffinityGroupForAttachingVmMsg) {
            handle((APIGetCandidateAffinityGroupForAttachingVmMsg) msg);
        } else if (msg instanceof APIGetCandidateAffinityGroupForCreatingVmMsg) {
            handle((APIGetCandidateAffinityGroupForCreatingVmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetCandidateAffinityGroupForCreatingVmMsg msg) {
        APIGetCandidateAffinityGroupForCreatingVmReply reply = new APIGetCandidateAffinityGroupForCreatingVmReply();

        List<String> clusterUuids = new ArrayList<String>();

        List<String> hostUuids = new ArrayList<String>();
        if (msg.getClusterUuid() == null || msg.getClusterUuid().isEmpty()) {
            clusterUuids.addAll(Q.New(ClusterVO.class).select(ClusterVO_.uuid).eq(ClusterVO_.zoneUuid, msg.getZoneUuid()).listValues());
        } else {
            clusterUuids.add(msg.getClusterUuid());
        }

        if (msg.getHostUuid() != null && !msg.getHostUuid().isEmpty()) {
            hostUuids.add(msg.getHostUuid());
        } else {
            List<String> uuids = Q.New(HostVO.class).select(HostVO_.uuid).in(HostVO_.clusterUuid, clusterUuids).listValues();
            hostUuids.addAll(uuids);
        }

        if (hostUuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<AffinityGroupVO> groupVOS = new ArrayList<>();

        List<AffinityGroupVO> vos = ResourceHelper.findOwnResources(AffinityGroupVO.class, msg.getSession().getAccountUuid(),
                q -> q.eq(AffinityGroupVO_.state, AffinityGroupState.Enabled)
                        .eq(AffinityGroupVO_.appliance, AffinityGroupAppliance.CUSTOMER.toString()));

        for (AffinityGroupVO vo : vos) {
            if (vo.getPolicy() == AffinityGroupPolicy.ANTIHARD || vo.getPolicy() == AffinityGroupPolicy.AFFINITYHARD) {

                List<String> uuids = SQL.New("select vm.hostUuid from AffinityGroupUsageVO usageVO, VmInstanceVO vm " +
                        "where usageVO.resourceUuid = vm.uuid and usageVO.resourceType = :resourceType and usageVO.affinityGroupUuid = :uuid")
                        .param("resourceType", VmInstanceVO.class.getSimpleName())
                        .param("uuid", vo.getUuid())
                        .list();

               List<String> differenceHostUuis = hostUuids.stream().filter(uuid -> !uuids.contains(uuid)).collect(Collectors.toList());

               if (differenceHostUuis.size() > 0) {
                   groupVOS.add(vo);
               }

            } else {
                groupVOS.add(vo);
            }
        }

        reply.setInventories(AffinityGroupInventory.valueOf(groupVOS));
        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateAffinityGroupForAttachingVmMsg msg) {

        APIGetCandidateAffinityGroupForAttachingVmReply reply = new APIGetCandidateAffinityGroupForAttachingVmReply();
        String agUuid = Q.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.resourceUuid, msg.getVmUuid())
                .select(AffinityGroupUsageVO_.affinityGroupUuid).findValue();
        List<AffinityGroupInventory> invs = new ArrayList<AffinityGroupInventory>();
        if (agUuid != null) {
            reply.setInventories(invs);
            bus.reply(msg, reply);
            return;
        }

        List<AffinityGroupVO> groupVOS = new ArrayList<>();

        List<AffinityGroupVO> vos = ResourceHelper.findOwnResources(AffinityGroupVO.class, msg.getSession().getAccountUuid(),
                q -> q.eq(AffinityGroupVO_.state, AffinityGroupState.Enabled)
                        .eq(AffinityGroupVO_.appliance, AffinityGroupAppliance.CUSTOMER.toString()));

        VmInstanceVO vm = dbf.findByUuid(msg.getVmUuid(), VmInstanceVO.class);

        for (AffinityGroupVO vo : vos) {
            if (vo.getPolicy() == AffinityGroupPolicy.ANTIHARD || vo.getPolicy() == AffinityGroupPolicy.AFFINITYHARD) {
                List<String> vmUuids = Q.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.affinityGroupUuid, vo.getUuid())
                        .select(AffinityGroupUsageVO_.resourceUuid).listValues();

                if (vmUuids == null || vmUuids.isEmpty()) {
                    groupVOS.add(vo);
                    continue;
                }

                if (!Q.New(VmInstanceVO.class).eq(VmInstanceVO_.hostUuid, vm.getHostUuid())
                                .in(VmInstanceVO_.uuid, vmUuids).isExists()) {
                    groupVOS.add(vo);
                }

            } else {
                groupVOS.add(vo);
            }
        }
        reply.setInventories(AffinityGroupInventory.valueOf(groupVOS));
        bus.reply(msg, reply);

    }

    private void passThrough(AffinityGroupMessage msg) {
        AffinityGroupVO vo = Q.New(AffinityGroupVO.class).eq(AffinityGroupVO_.uuid, msg.getAffinityGroupUuid()).find();
        if (vo == null) {
            throw new OperationFailureException(argerr("cannot find the affinity group[uuid:%s], it may have been deleted", msg.getAffinityGroupUuid()));
        }

        AffinityGroupBase ag = new AffinityGroupBase(vo);
        ag.handleMessage((Message) msg);
    }

    private void handle(CreateAffinityGroupMsg msg) {
        CreateAffinityGroupReply reply = new CreateAffinityGroupReply();
        AffinityGroupVO vo = new AffinityGroupVO();

        vo.setName(msg.getName());
        vo.setUuid(Platform.getUuid());
        vo.setDescription(msg.getDescription());
        vo.setPolicy(AffinityGroupPolicy.valueOf(msg.getPolicy().toUpperCase()));
        vo.setVersion(AffinityGroupConstants.DEFAULT_VERSION);
        vo.setType(msg.getType() == null ? AffinityGroupType.HOST : AffinityGroupType.valueOf(msg.getType().toUpperCase()));
        vo.setAppliance(msg.getApplianceType());
        vo.setState(AffinityGroupState.Enabled);
        vo.setAccountUuid(msg.getAccountUuid());
        vo.setZoneUuid(msg.getZoneUuid());

        AffinityGroupSubTypeFactory subTypeFactory =
                getAffinityGroupSubTypeFactory(msg.getSubType() == null ? AffinityGroupSubType.valueOf(VmSchedulingRuleConstants.VM_SCHEDULING_RULE_TYPE)
                        : AffinityGroupSubType.valueOf(msg.getSubType()));

        vo = subTypeFactory.persistAffinityGroup(msg, vo);

        reply.setAffinityGroup(AffinityGroupInventory.valueOf(vo));
        bus.reply(msg, reply);
    }

    private void handle(APICreateAffinityGroupMsg msg) {
        APICreateAffinityGroupEvent evt = new APICreateAffinityGroupEvent(msg.getId());
        AffinityGroupVO vo = new AffinityGroupVO();

        vo.setName(msg.getName());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setDescription(msg.getDescription());
        vo.setPolicy(AffinityGroupPolicy.valueOf(msg.getPolicy().toUpperCase()));
        vo.setVersion(AffinityGroupConstants.DEFAULT_VERSION);
        vo.setType(msg.getType() == null ? AffinityGroupType.HOST : AffinityGroupType.valueOf(msg.getType().toUpperCase()));
        vo.setAppliance(AffinityGroupAppliance.CUSTOMER.toString());
        vo.setState(AffinityGroupState.Enabled);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setZoneUuid(msg.getZoneUuid());

        AffinityGroupSubTypeFactory subTypeFactory =
                getAffinityGroupSubTypeFactory(msg.getSubType() == null ? AffinityGroupSubType.valueOf(VmSchedulingRuleConstants.VM_SCHEDULING_RULE_TYPE)
                        : AffinityGroupSubType.valueOf(msg.getSubType()));

        vo = subTypeFactory.persistAffinityGroup(msg, vo);

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), AffinityGroupVO.class.getSimpleName());

        evt.setInventory(AffinityGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AffinityGroupConstants.SERVICE_ID);
    }

    public AffinityGroupRatingFactory getAffinityGroupRating(AffinityGroupType type){
        if (AffinityGroupType.HOST == type && AffinityGroupGlobalProperty.AFFINITY_GROUP_HOST_COUNT_ALL_VMS) {
            type = AffinityGroupType.HOSTALL;
        }

        return factories.get(type.toString());
    }

    public AffinityGroupSubTypeFactory getAffinityGroupSubTypeFactory(AffinityGroupSubType type) {
        return subTypeFactoryMap.get(type.toString());
    }


    private void populateExtensions() {
        for (AffinityGroupRatingFactory ext : pluginRgty.getExtensionList(AffinityGroupRatingFactory.class)) {
            AffinityGroupRatingFactory old = factories.get(ext.getAffinityGroupType().toString());
            DebugUtils.Assert(old == null, String.format("duplicate AffinityGroupRating"));
            factories.put(ext.getAffinityGroupType().toString(), ext);
        }

        for (AffinityGroupSubTypeFactory ext : pluginRgty.getExtensionList(AffinityGroupSubTypeFactory.class)) {
            AffinityGroupSubTypeFactory old = subTypeFactoryMap.get(ext.getAffinityGroupSubType().toString());
            DebugUtils.Assert(old == null, String.format("duplicate AffinityGroupSubType"));
            subTypeFactoryMap.put(ext.getAffinityGroupSubType().toString(), ext);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void deleteAffinityGroupUsage(String vmUuid){
        /* vm can be added to ONLY 1 affinityGroup */
        AffinityGroupUsageVO usageVo = Q.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.resourceUuid, vmUuid).limit(1).find();
        if (usageVo == null){
            logger.debug(String.format("Vm [uuid:%s] is not in any affinityGroup", vmUuid));
            return;
        }
        dbf.remove(usageVo);
    }

    public void addVmToAffinityGroupUsage (String affinityGroupUuid, String vmUuid) {
        AffinityGroupUsageVO usageVo = Q.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.affinityGroupUuid, affinityGroupUuid).
                eq(AffinityGroupUsageVO_.resourceUuid, vmUuid).find();
        if (usageVo != null){
            return;
        }

        AffinityGroupUsageVO newVo = new AffinityGroupUsageVO();
        newVo.setUuid(Platform.getUuid());
        newVo.setAffinityGroupUuid(affinityGroupUuid);
        newVo.setResourceType(VmInstanceVO.class.getSimpleName());
        newVo.setResourceUuid(vmUuid);
        dbf.persist(newVo);
    }

    public void createVmSystemTagForAffinityGroup(String agUuid, String vmUuid) {
        PatternedSystemTag tag =  AffinityGroupSystemTags.AFFINITY_GROUP_UUID;

        /* remove vm old nonherent systemTag  */
        if (tag.getTokenByResourceUuid(vmUuid, AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN) != null) {
            AffinityGroupSystemTags.AFFINITY_GROUP_UUID.delete(vmUuid);
        }

        SystemTagCreator creator = AffinityGroupSystemTags.AFFINITY_GROUP_UUID.newSystemTagCreator(vmUuid);
        creator.inherent = true;
        creator.recreate = true;
        creator.setTagByTokens(map(e(AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN, agUuid)));
        creator.create();
    }

    @Override
    public void attachVmInstanceToAffinityGroup(String vmUuid, String affinityGroupUuid) {
        if (affinityGroupUuid == null) {
            /* add limit(1) to avoid sometime db error: there are multiple VROUTER records in db  */
            List<String> validTypes = new ArrayList<>();
            validTypes.add(AffinityGroupAppliance.VROUTER.toString());
            validTypes.add(AffinityGroupAppliance.VROUTER_HA.toString());
            String appAffinityGroupUuid = Q.New(AffinityGroupVO.class)
                    .eq(AffinityGroupVO_.resourceName, AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP)
                    .in(AffinityGroupVO_.appliance, validTypes)
                    .eq(AffinityGroupVO_.type, AffinityGroupType.HOST)
                    .eq(AffinityGroupVO_.policy, AffinityGroupPolicy.ANTISOFT)
                    .select(AffinityGroupVO_.uuid).limit(1).findValue();
            if (appAffinityGroupUuid == null) {
                logger.warn(String.format("!!!AffinityGroup for virtual router is deleted"));
                return;
            }
            affinityGroupUuid = appAffinityGroupUuid;
        }

        createVmSystemTagForAffinityGroup(affinityGroupUuid, vmUuid);
        addVmToAffinityGroupUsage(affinityGroupUuid, vmUuid);
    }

    @Override
    public void detachVmInstanceFromAffinityGroup(String vmUuid) {
        AffinityGroupSystemTags.AFFINITY_GROUP_UUID.deleteInherentTag(vmUuid);
        SQL.New(AffinityGroupUsageVO.class).eq(AffinityGroupUsageVO_.resourceUuid, vmUuid).delete();
    }

    @Override
    public ErrorCode handleSystemTag(String vmUuid, List<String> tags) {
        PatternedSystemTag tag =  AffinityGroupSystemTags.AFFINITY_GROUP_UUID;
        String token = AffinityGroupSystemTags.AFFINITY_GROUP_UUID_TOKEN;

        String agUuid = SystemTagUtils.findTagValue(tags, tag, token);
        if(StringUtils.isEmpty(agUuid)){
            return null;
        }

        createVmSystemTagForAffinityGroup(agUuid, vmUuid);
        addVmToAffinityGroupUsage(agUuid, vmUuid);
        return null;
    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {

    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {
        deleteAffinityGroupUsage(inv.getUuid());
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        deleteAffinityGroupUsage(inv.getUuid());
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new AffinityGroupNumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateAffinityGroupMsg.class)
                .addFixedRequiredSize(AffinityGroupConstants.AFFINITYGROUP_NUM, 1L));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(AffinityGroupVO.class)
                        .eq(AffinityGroupVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(AffinityGroupConstants.AFFINITYGROUP_NUM));
        return list(quota);
    }
}
