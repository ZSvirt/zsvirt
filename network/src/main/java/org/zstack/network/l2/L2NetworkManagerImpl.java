package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.FilterAttachableL3NetworkExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;

public class L2NetworkManagerImpl extends AbstractService implements L2NetworkManager, FilterAttachableL3NetworkExtensionPoint {
    private static final CLogger logger = Utils.getLogger(L2NetworkManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ResourceConfigFacade rcf;

    private Map<String, L2NetworkFactory> l2NetworkFactories = Collections.synchronizedMap(new HashMap<String, L2NetworkFactory>());
    private Map<L2NetworkType, Map<HypervisorType, L2NetworkRealizationExtensionPoint>> realizationExts = new HashMap<>();
    private final Map<L2ProviderType, Map<L2NetworkType, L2NetworkRealizationExtensionPoint>> l2ProviderMap = new HashMap<>();
    private Map<L2NetworkType, Map<HypervisorType, L2NetworkAttachClusterExtensionPoint>> attachClusterExts = new HashMap<>();
    private List<L2NetworkCreateExtensionPoint> createExtensions = new ArrayList<L2NetworkCreateExtensionPoint>();
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

    static {
        allowedMessageAfterSoftDeletion.add(L2NetworkDeletionMsg.class);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof L2NetworkMessage) {
            passThrough((L2NetworkMessage)msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateL2NetworkMsg) {
            handle((APICreateL2NetworkMsg)msg);
        } else if (msg instanceof APIGetL2NetworkTypesMsg) {
            handle((APIGetL2NetworkTypesMsg) msg);
        } else if (msg instanceof APIGetVSwitchTypesMsg) {
            handle((APIGetVSwitchTypesMsg) msg);
        } else if (msg instanceof APIGetCandidateL2NetworksForAttachingClusterMsg) {
            handle((APIGetCandidateL2NetworksForAttachingClusterMsg)msg);
        } else if (msg instanceof APIGetCandidateClustersForAttachingL2NetworkMsg) {
            handle((APIGetCandidateClustersForAttachingL2NetworkMsg)msg);
        } else if (msg instanceof L2NetworkMessage) {
            passThrough((L2NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetCandidateClustersForAttachingL2NetworkMsg msg) {
        APIGetCandidateClustersForAttachingL2NetworkReply reply = new APIGetCandidateClustersForAttachingL2NetworkReply();
        reply.setInventories(getClusterCandidates(msg));
        bus.reply(msg, reply);
    }

    private List<ClusterInventory> getClusterCandidates(APIGetCandidateClustersForAttachingL2NetworkMsg msg) {
        String l2Uuid = msg.getL2NetworkUuid();
        L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();

        if (l2NetworkVO == null || l2NetworkVO.getType().equals(L2NetworkConstant.VXLAN_NETWORK_TYPE)) {
            return new ArrayList<>();
        }

        List<String> attachedClusterUuids = SQL.New("select distinct ref.clusterUuid from L2NetworkVO l2, L2NetworkClusterRefVO ref where" +
                        " l2.uuid = ref.l2NetworkUuid" +
                        " and l2.uuid = :l2Uuid")
                .param("l2Uuid", l2NetworkVO.getUuid())
                .list();

        List<ClusterVO> clusterVOS;
        if (msg.getClusterTypes() == null || msg.getClusterTypes().isEmpty()) {
            clusterVOS = Q.New(ClusterVO.class).eq(ClusterVO_.zoneUuid, l2NetworkVO.getZoneUuid()).list();
        } else {
            clusterVOS = Q.New(ClusterVO.class).eq(ClusterVO_.zoneUuid, l2NetworkVO.getZoneUuid()).in(ClusterVO_.type, msg.getClusterTypes()).list();
        }
        if (clusterVOS.isEmpty()) {
            return new ArrayList<>();
        }
        clusterVOS.removeIf(clusterVO -> attachedClusterUuids.contains(clusterVO.getUuid()));

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(L2NetworkCandidateFilterExtensionPoint.class),
                ext -> ext.filterClusterCandidates(clusterVOS, l2NetworkVO));

        if (l2NetworkVO.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE) ||
                l2NetworkVO.getType().equals(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE)) {
            clusterVOS.removeIf(cluster -> !canAttachL2ToThisCluster(l2NetworkVO, cluster));
        } else if (L2NetworkType.valueOf(l2NetworkVO.getType()).isAttachToAllHosts()) {
            clusterVOS.removeIf(cluster -> !cluster.getHypervisorType().equals("KVM"));
        }

        return ClusterInventory.valueOf(clusterVOS);
    }

    private boolean ovsDpdkSupport(ClusterVO clusterVO) {
        String memAccessMode = "private";

        ResourceConfig memAccess = rcf.getResourceConfig("premiumCluster.memAccess.mode");
        if (memAccess != null) {
            memAccessMode = memAccess.getResourceConfigValue(clusterVO.getUuid(), String.class);
        }

        ResourceConfig numa = rcf.getResourceConfig("vm.numa");
        boolean isNumaEnable = numa.getResourceConfigValue(clusterVO.getUuid(), Boolean.class);

        boolean isOvsDpdkSup = false;
        ResourceConfig ovsDpdkSup = rcf.getResourceConfig("premiumCluster.network.ovsdpdk");
        if (ovsDpdkSup != null) {
            isOvsDpdkSup = ovsDpdkSup.getResourceConfigValue(clusterVO.getUuid(), Boolean.class);
        }

        if (memAccessMode.equals("private") || !isNumaEnable || !isOvsDpdkSup){
            return false;
        }

        return true;
    }

    private boolean canAttachL2ToThisCluster(L2NetworkVO l2NetworkVO, ClusterVO clusterVO) {

        String vSwitchType = l2NetworkVO.getvSwitchType();
        if (vSwitchType.equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK) && !ovsDpdkSupport(clusterVO)) {
            return false;
        }

        StringBuilder sqlBuilder = new StringBuilder();
        if (l2NetworkVO.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)) {
            sqlBuilder.append("select l2 from L2VlanNetworkVO l2,L2NetworkClusterRefVO ref where");
        } else {
            sqlBuilder.append("select l2 from L2NetworkVO l2,L2NetworkClusterRefVO ref where");
        }
        sqlBuilder.append(" l2.type = :l2Type and");
        sqlBuilder.append(" l2.uuid = ref.l2NetworkUuid and ref.clusterUuid = :clusterUuid");


        if (l2NetworkVO.getType().equals(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE)) {
            List<L2NetworkVO> attachedL2VOS = SQL.New(sqlBuilder.toString())
                    .param("l2Type",L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE)
                    .param("clusterUuid", clusterVO.getUuid())
                    .list();
            return attachedL2VOS.stream().noneMatch(l2 -> l2.getPhysicalInterface().equals(l2NetworkVO.getPhysicalInterface()));
        } else if (l2NetworkVO.getType().equals(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)) {
            L2VlanNetworkVO l2VlanNetworkVO = Q.New(L2VlanNetworkVO.class).eq(L2VlanNetworkVO_.uuid, l2NetworkVO.getUuid()).find();
            List<L2VlanNetworkVO> attachedL2VOS = SQL.New(sqlBuilder.toString())
                    .param("l2Type",L2NetworkConstant.L2_VLAN_NETWORK_TYPE)
                    .param("clusterUuid", clusterVO.getUuid())
                    .list();
            return attachedL2VOS.stream().noneMatch(l2 -> (l2.getPhysicalInterface().equals(l2VlanNetworkVO.getPhysicalInterface()) &&
                    l2.getVlan() == l2VlanNetworkVO.getVlan()));
        }
        return false;
    }

    private void handle(APIGetCandidateL2NetworksForAttachingClusterMsg msg) {
        APIGetCandidateL2NetworksForAttachingClusterReply reply = new APIGetCandidateL2NetworksForAttachingClusterReply();

        reply.setInventories(getCandidateL2ForAttachingCluster(msg.getClusterUuid()));
        bus.reply(msg, reply);
    }

    private List<L2NetworkData> getCandidateL2ForAttachingCluster(String clusterUuid) {
        ClusterVO clusterVO = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, clusterUuid).find();
        if (clusterVO == null) {
            return new ArrayList<>();
        }

        List<L2NetworkVO> l2s = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.zoneUuid, clusterVO.getZoneUuid()).list();
        if (l2s.isEmpty()) {
            return new ArrayList<>();
        }

        List<L2NetworkVO> attachClusterL2s = SQL.New("select distinct l2 from L2NetworkVO l2, L2NetworkClusterRefVO ref where" +
                        " l2.uuid = ref.l2NetworkUuid" +
                        " and ref.clusterUuid = :clusterUuid")
                .param("clusterUuid", clusterVO.getUuid())
                .list();
        //filter l2 attached to cluster
        l2s.removeIf(l2 -> attachClusterL2s.stream()
                .anyMatch(attachedL2s -> attachedL2s.getUuid().equals(l2.getUuid())));

        //filter l2 attached to different type cluster
        List<String> attachedDifferentTypeClusterL2Uuids;
        if (!clusterVO.getType().equals("vmware")) {
            attachedDifferentTypeClusterL2Uuids = SQL.New("select distinct l2.uuid from L2NetworkVO l2, L2NetworkClusterRefVO ref, ClusterVO cluster where" +
                            " l2.uuid = ref.l2NetworkUuid" +
                            " and ref.clusterUuid = cluster.uuid" +
                            " and cluster.type = :clusterType")
                    .param("clusterType", "vmware")
                    .list();
        } else {
            attachedDifferentTypeClusterL2Uuids = SQL.New("select distinct l2.uuid from L2NetworkVO l2, L2NetworkClusterRefVO ref where" +
                            " l2.uuid = ref.l2NetworkUuid" +
                            " and ref.clusterUuid is not null")
                    .list();
        }
        l2s.removeIf(l2 -> attachedDifferentTypeClusterL2Uuids.contains(l2.getUuid()));

        if (!ovsDpdkSupport(clusterVO)) {
            final List<String> DpdkL2Uuids = SQL.New("select distinct l2.uuid from L2NetworkVO l2 where" +
                            " l2.vSwitchType = :l2vSwitchType")
                    .param("l2vSwitchType", "OvsDpdk")
                    .list();
            l2s.removeIf(l2 -> DpdkL2Uuids.contains(l2.getUuid()));
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(L2NetworkCandidateFilterExtensionPoint.class),
                ext -> ext.filterL2NetworkCandidates(l2s, clusterVO));

        List<String> excludedTypes = Arrays.asList(L2NetworkConstant.VXLAN_NETWORK_TYPE,
                L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE,
                L2NetworkConstant.L2_TF_NETWORK_TYPE);
        l2s.removeIf(l2 -> excludedTypes.contains(l2.getType()));

        if (!clusterVO.getHypervisorType().equals("KVM")) {
            l2s.removeIf(l2 -> L2NetworkConstant.VXLAN_NETWORK_POOL_TYPE.equals(l2.getType()) ||
                    L2NetworkConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE.equals(l2.getType()));
        }

        if (!attachClusterL2s.isEmpty()) {
            l2s.removeIf(l2 -> {
                if (L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE.equals(l2.getType()) ||
                        L2NetworkConstant.L2_VLAN_NETWORK_TYPE.equals(l2.getType())) {
                    return attachClusterL2s.stream()
                            .anyMatch(attachedL2 -> Objects.equals(attachedL2.getPhysicalInterface(), l2.getPhysicalInterface()) &&
                                    Objects.equals(attachedL2.getVirtualNetworkId(), l2.getVirtualNetworkId()));
                }
                return false;
            });
        }

        return getL2DateResult(l2s);
    }

    private List<L2NetworkData> getL2DateResult(List<L2NetworkVO> l2s) {
        List<L2NetworkData> ret = new ArrayList<>();
        for (L2NetworkVO l2NetworkVO : l2s) {
            L2NetworkData l2NetworkData = new L2NetworkData();
            l2NetworkData.setUuid(l2NetworkVO.getUuid());
            l2NetworkData.setName(l2NetworkVO.getName());
            l2NetworkData.setPhysicalInterface(l2NetworkVO.getPhysicalInterface());
            l2NetworkData.setType(l2NetworkVO.getType());
            l2NetworkData.setVirtualNetworkId(l2NetworkVO.getVirtualNetworkId());
            l2NetworkData.setZoneUuid(l2NetworkVO.getZoneUuid());
            l2NetworkData.setDescription(l2NetworkVO.getDescription());
            l2NetworkData.setCreateDate(l2NetworkVO.getCreateDate());
            l2NetworkData.setLastOpDate(l2NetworkVO.getLastOpDate());
            ret.add(l2NetworkData);
        }
        return ret;
    }

    private void handle(APIGetVSwitchTypesMsg msg) {
        List<String> vSwitchTypes = new ArrayList<String>();

        vSwitchTypes.addAll(VSwitchType.getAllTypeNames());
        APIGetVSwitchTypesReply reply = new APIGetVSwitchTypesReply();
        reply.setVSwitchTypes(vSwitchTypes);
        bus.reply(msg, reply);
    }

    private void handle(APIGetL2NetworkTypesMsg msg) {
        List<String> types = new ArrayList<String>();
        types.addAll(L2NetworkType.getAllTypeNames());
        APIGetL2NetworkTypesReply reply = new APIGetL2NetworkTypesReply();
        reply.setL2NetworkTypes(types);
        bus.reply(msg, reply);
    }

	private void passThrough(L2NetworkMessage msg) {
        Message amsg = (Message) msg;
        L2NetworkVO vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            L2NetworkEO eo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkEO.class);
            vo = ObjectUtils.newAndCopy(eo, L2NetworkVO.class);
        }

        if (vo == null) {
            ErrorCode errCode = err(SysErrors.RESOURCE_NOT_FOUND, "unable to find L2Network[uuid:%s], it may have been deleted", msg.getL2NetworkUuid());
            bus.replyErrorByMessageType((Message)msg, errCode);
            return;
        }

        L2NetworkFactory factory = getL2NetworkFactory(L2NetworkType.valueOf(vo.getType()));
        L2Network nw = factory.getL2Network(vo);
        nw.handleMessage(amsg);
    }

    private void handle(APICreateL2NetworkMsg msg) {
    	for (L2NetworkCreateExtensionPoint extp : createExtensions) {
    		try {
				extp.beforeCreateL2Network(msg);
			} catch (NetworkException e) {
				APICreateL2NetworkEvent evt = new APICreateL2NetworkEvent(msg.getId());
                evt.setError(err(SysErrors.CREATE_RESOURCE_ERROR, "unable to create l2network[name:%s, type:%s], %s", msg.getName(), msg.getType(), e.getMessage()));
                logger.warn(evt.getError().getDetails(), e);
				bus.publish(evt);
				return;
			}
    	}

        L2NetworkType type = L2NetworkType.valueOf(msg.getType());
    	VSwitchType vSwitchType = VSwitchType.valueOf(msg.getvSwitchType());
        L2NetworkFactory factory = getL2NetworkFactory(type);
        L2NetworkVO vo = new L2NetworkVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setPhysicalInterface(msg.getPhysicalInterface());
        vo.setType(type.toString());
        vo.setvSwitchType(vSwitchType.toString());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        factory.createL2Network(vo, msg, new ReturnValueCompletion<L2NetworkInventory>(msg) {
            @Override
            public void success(L2NetworkInventory returnValue) {
                tagMgr.createTagsFromAPICreateMessage(msg, returnValue.getUuid(), L2NetworkVO.class.getSimpleName());

                for (L2NetworkCreateExtensionPoint extp : createExtensions) {
                    try {
                        extp.afterCreateL2Network(returnValue);
                    } catch (Exception e) {
                        logger.warn(String.format("unhandled exception happened when calling %s", extp.getClass().getName()), e);
                    }
                }

                APICreateL2NetworkEvent evt = new APICreateL2NetworkEvent(msg.getId());
                evt.setInventory(returnValue);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                APICreateL2NetworkEvent evt = new APICreateL2NetworkEvent(msg.getId());
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });

    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(L2NetworkConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public L2NetworkFactory getL2NetworkFactory(L2NetworkType type) {
        L2NetworkFactory factory = l2NetworkFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find L2NetworkFactory for type(%s)", type));
        }

        return factory;
    }

    public L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, HypervisorType hvType) {
        Map<HypervisorType, L2NetworkRealizationExtensionPoint> map = realizationExts.get(l2Type);
        if (map == null) {
            throw new IllegalArgumentException(String.format("Cannot find L2NetworkRealizationExtensionPoint supporting L2NetworkType[%s]", l2Type));
        }

        L2NetworkRealizationExtensionPoint extp = map.get(hvType);
        if (extp == null) {
            throw new IllegalArgumentException(String.format("Cannot find L2NetworkRealizationExtensionPoint for L2NetworkType[%s] supporting hypervisor[%s]", l2Type, hvType));
        }

        return extp;
    }

    @Override
    public L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, L2ProviderType providerType) {
        Map<L2NetworkType, L2NetworkRealizationExtensionPoint> map = l2ProviderMap.get(providerType);
        if (map == null) {
            throw new IllegalArgumentException(String.format("Cannot find L2NetworkRealizationExtensionPoint supporting L2ProviderType[%s]", providerType));
        }

        L2NetworkRealizationExtensionPoint extp = map.get(l2Type);
        if (extp == null) {
            throw new IllegalArgumentException(String.format("Cannot find L2NetworkRealizationExtensionPoint for L2ProviderType[%s] supporting L2NetworkType[%s]",
                    providerType, l2Type));
        }

        return extp;
    }

    @Override
    public L2NetworkRealizationExtensionPoint getRealizationExtension(L2NetworkType l2Type, HypervisorType hvType, String providerType) {
        if (providerType == null) {
            return getRealizationExtension(l2Type, hvType);
        }

        return getRealizationExtension(l2Type, L2ProviderType.valueOf(providerType));
    }

    @Override
    public L2NetworkAttachClusterExtensionPoint getAttachClusterExtension(L2NetworkType l2Type, HypervisorType hvType) {
        Map<HypervisorType, L2NetworkAttachClusterExtensionPoint> map = attachClusterExts.get(l2Type);
        if (map == null) {
            logger.debug(String.format("Cannot find L2NetworkAttachClusterExtensionPoint supporting L2NetworkType[%s]", l2Type));
            return null;
        }

        L2NetworkAttachClusterExtensionPoint extp = map.get(hvType);
        if (extp == null) {
            logger.debug(String.format("Cannot find L2NetworkAttachClusterExtensionPoint for L2NetworkType[%s] supporting hypervisor[%s]", l2Type, hvType));
            return null;
        }

        return extp;
    }

    private void populateExtensions() {
        for (L2NetworkFactory f : pluginRgty.getExtensionList(L2NetworkFactory.class)) {
            L2NetworkFactory old = l2NetworkFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate L2NetworkFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getType()));
            }
            l2NetworkFactories.put(f.getType().toString(), f);
        }

        for (L2NetworkRealizationExtensionPoint extp : pluginRgty.getExtensionList(L2NetworkRealizationExtensionPoint.class)) {
            L2ProviderType providerType = extp.getL2ProviderType();
            // for backends that do not support l2ProviderType
            if (providerType == null) {
                Map<HypervisorType, L2NetworkRealizationExtensionPoint> map = realizationExts.computeIfAbsent(
                        extp.getSupportedL2NetworkType(), k -> new HashMap<HypervisorType, L2NetworkRealizationExtensionPoint>(1));
                map.put(extp.getSupportedHypervisorType(), extp);
                continue;
            }

            Map<L2NetworkType, L2NetworkRealizationExtensionPoint> map = l2ProviderMap.computeIfAbsent(
                    providerType, k -> new HashMap<L2NetworkType, L2NetworkRealizationExtensionPoint>());

            L2NetworkType l2Type = extp.getSupportedL2NetworkType();
            logger.debug(String.format("add exp: %s for l2 type: %s , providerType: %s", extp.getClass().getSimpleName(), l2Type, providerType));
            map.put(l2Type, extp);
        }

        for (L2NetworkAttachClusterExtensionPoint extp : pluginRgty.getExtensionList(L2NetworkAttachClusterExtensionPoint.class)) {
            Map<HypervisorType, L2NetworkAttachClusterExtensionPoint> map =
                    attachClusterExts.computeIfAbsent(extp.getSupportedL2NetworkType(), k -> new HashMap<>(1));
            map.put(extp.getSupportedHypervisorType(), extp);
        }

        createExtensions = pluginRgty.getExtensionList(L2NetworkCreateExtensionPoint.class);
    }

    @Override
    public List<L3NetworkInventory> filterAttachableL3Network(VmInstanceInventory vm, List<L3NetworkInventory> l3s) {
        String hostUuid = vm.getHostUuid() != null ? vm.getHostUuid() : vm.getLastHostUuid();
        List<String> l2Uuids = l3s.stream().map(L3NetworkInventory::getL2NetworkUuid).distinct().collect(Collectors.toList());
        List<L3NetworkInventory> rets = new ArrayList<>(l3s);
        if (l2Uuids.isEmpty()) {
            return rets;
        }

        List<String> excludeL2Uuids = L2NetworkHostUtils.getExcludeL2Uuids(l2Uuids, hostUuid);
        rets.removeIf(l3 -> excludeL2Uuids.contains(l3.getL2NetworkUuid()));
        return rets;
    }
}
