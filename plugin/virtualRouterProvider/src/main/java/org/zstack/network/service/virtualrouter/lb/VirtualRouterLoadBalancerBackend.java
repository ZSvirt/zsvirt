package org.zstack.network.service.virtualrouter.lb;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.acl.AccessControlListEntryVO;
import org.zstack.header.acl.AccessControlListEntryVO_;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.vyos.VyosGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AgentResponse;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipBackend;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 8/9/2015.
 */
public class VirtualRouterLoadBalancerBackend extends AbstractVirtualRouterBackend
        implements LoadBalancerBackend, GlobalApiMessageInterceptor, ApiMessageInterceptor, VirtualRouterHaGetCallbackExtensionPoint,
        VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterLoadBalancerBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    private VirtualRouterVipBackend vipVrBkd;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private LbConfigProxy proxy;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private LoadBalancerManager lbMgr;
    @Autowired
    private VipConfigProxy vipProxy;
    @Autowired
    private ResourceConfigFacade rcf;

    private static final String REFRESH_CERTIFICATE_TASK = "refreshCertificate";
    private static final String DELETE_CERTIFICATE_TASK = "deleteCertificate";
    private static final String REFRESH_LB_TASK = "refreshLb";
    private static final String DESTROY_LB_TASK = "destroyLb";

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIAddVmNicToLoadBalancerMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddVmNicToLoadBalancerMsg) {
            validate((APIAddVmNicToLoadBalancerMsg) msg);
        }

        return msg;
    }

    protected String getLoadLancerServiceProvider(List<String> l3Uuids) {
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                l3Uuids.get(0), LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
        return providerType.toString();
    }

    @Transactional(readOnly = true)
    private void validate(APIAddVmNicToLoadBalancerMsg msg) {
        LoadBalancerListenerVO listenerVO = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
        LoadBalancerServerGroupVO groupVO = lbMgr.getDefaultServerGroup(listenerVO);
        List<String> attachedVmNicUuids = new ArrayList<>();
        if (groupVO != null) {
            attachedVmNicUuids = groupVO.getLoadBalancerServerGroupVmNicRefs().stream()
                    .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList());
        }

        attachedVmNicUuids.addAll(msg.getVmNicUuids());

        Set<String> l3NetworkUuids = new HashSet<>(
                Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid)
                        .in(VmNicVO_.uuid, attachedVmNicUuids)
                        .listValues());

        Set<String> vrUuids = new HashSet<>(
                Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                        .in(VmNicVO_.l3NetworkUuid, l3NetworkUuids)
                        .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                        .listValues());

        boolean valid = true;
        if (vrUuids.size() == 2 ) {
            if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(msg.getLoadBalancerUuid())
                    && vrUuids.stream().anyMatch(uuid -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(uuid))) {
                logger.debug(String.format(
                        "there are two virtual routers[uuids:%s] on l3 networks[uuids:%s] which vmnics[uuids:%s]" +
                                "attached", vrUuids, l3NetworkUuids, attachedVmNicUuids));
                valid = true;
            } else if (isVirtualRouterHaPair(new ArrayList<>(vrUuids))){
                valid = true;
            } else {
                valid = false;
            }
        } else if (vrUuids.size() > 1) {
            valid = false;
        }
        if (!valid) {
            throw new ApiMessageInterceptionException(argerr(
                    "new add vm nics[uuids:%s] and attached vmnics are not on the same vrouter, " +
                            "they are on vrouters[uuids:%s]", msg.getVmNicUuids(), vrUuids));
        }

        List<String> peerL3NetworkUuids = SQL.New("select peer.l3NetworkUuid " +
                "from LoadBalancerVO lb, VipVO vip, VipPeerL3NetworkRefVO peer " +
                "where lb.vipUuid = vip.uuid " +
                "and vip.uuid = peer.vipUuid " +
                "and lb.uuid = :lbUuid")
                .param("lbUuid", msg.getLoadBalancerUuid())
                .list();

        if (peerL3NetworkUuids == null || peerL3NetworkUuids.isEmpty()) {
            return;
        }

        List<String> requestVmNicsL3NetworkUuids = Q.New(VmNicVO.class)
                .select(VmNicVO_.l3NetworkUuid)
                .in(VmNicVO_.uuid, msg.getVmNicUuids())
                .listValues();

        requestVmNicsL3NetworkUuids.addAll(peerL3NetworkUuids);
        vrUuids = new HashSet<>(
                Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                        .in(VmNicVO_.l3NetworkUuid, requestVmNicsL3NetworkUuids)
                        .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                        .listValues());

        if (vrUuids.size() > 1 && !isVirtualRouterHaPair(new ArrayList<>(vrUuids))) {
            throw new ApiMessageInterceptionException(argerr(
                    "new add vm nics[uuids:%s] and peer l3s[uuids:%s] of loadbalancer[uuid: %s]'s vip are not on the same vrouter, " +
                            "they are on vrouters[uuids:%s]", msg.getVmNicUuids(), peerL3NetworkUuids, msg.getLoadBalancerUuid(), vrUuids));
        }
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findVirtualRouterVm(String lbUuid) {
        List<VirtualRouterVmVO> vrs = getAllVirtualRouters(lbUuid);
        if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(lbUuid)) {
            Optional<VirtualRouterVmInventory> vr = vrs.stream()
                    .filter(v -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(v.getUuid()))
                    .map(VirtualRouterVmInventory::valueOf)
                    .findFirst();

            return vr.orElse(null);
        }

        DebugUtils.Assert(vrs.size() <= 1, String.format("multiple virtual routers[uuids:%s] found",
                vrs.stream().map(ResourceVO::getUuid).collect(Collectors.toList())));
        return vrs.isEmpty() ? null : VirtualRouterVmInventory.valueOf(vrs.get(0));
    }

    @Transactional(readOnly = true)
    private VirtualRouterVmInventory findVirtualRouterVm(String lbUuid, List<String> vmNics) {
        List<VirtualRouterVmVO> vrs = getAllVirtualRouters(lbUuid);

        if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(lbUuid)) {
            Optional<VirtualRouterVmVO> vr = vrs.stream()
                    .filter(v -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(v.getUuid()))
                    .findFirst();

            if (!vr.isPresent()) {
                return null;
            }

            List<String> vmNicL3NetworkUuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, vmNics).listValues();

            VirtualRouterVmInventory vrInventory = VirtualRouterVmInventory.valueOf(vr.get());
            vmNicL3NetworkUuids.removeAll(vrInventory.getGuestL3Networks());

            if (!vmNicL3NetworkUuids.isEmpty()) {
                logger.debug(String.format("found l3 networks[uuids:%s] not attached to separate vr[uuid:%s] for loadbalancer[uuid:%s]",
                        vmNicL3NetworkUuids, vr.get().getUuid(), lbUuid));
                throw new CloudRuntimeException("not support separate vr with multiple networks vpc!");
            }
        }

        DebugUtils.Assert(vrs.size() <= 1, String.format("multiple virtual routers[uuids:%s] found",
                vrs.stream().map(ResourceVO::getUuid).collect(Collectors.toList())));
        return vrs.isEmpty() ? null : VirtualRouterVmInventory.valueOf(vrs.get(0));
    }

    public static class LbTO {
        String lbUuid;
        String listenerUuid;
        String vip;
        String publicNic;
        List<String> nicIps;
        int instancePort;
        int loadBalancerPort;
        String mode;
        List<String> parameters;
        String  certificateUuid;
        String securityPolicyType;
        List<ServerGroup> serverGroups;
        List<RedirectRule> redirectRules;

        static class ServerGroup {
            private String name;
            private String serverGroupUuid;
            private List<BackendServer> backendServers;
            private boolean isDefault = false;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getServerGroupUuid() {
                return serverGroupUuid;
            }

            public void setServerGroupUuid(String serverGroupUuid) {
                this.serverGroupUuid = serverGroupUuid;
            }

            public List<BackendServer> getBackendServers() {
                return backendServers;
            }

            public void setBackendServers(List<BackendServer> backendServers) {
                this.backendServers = backendServers;
            }

            public boolean isDefault() {
                return isDefault;
            }

            public void setDefault(boolean aDefault) {
                isDefault = aDefault;
            }
        }

        static class BackendServer {
            private String ip;
            private long weight;

            public BackendServer(String ip, long weight) {
                this.ip = ip;
                this.weight = weight;
            }

            public String getIp() {
                return ip;
            }

            public void setIp(String ip) {
                this.ip = ip;
            }

            public long getWeight() {
                return weight;
            }

            public void setWeight(long weight) {
                this.weight = weight;
            }
        }

        static class RedirectRule {
            private String redirectRuleUuid;
            private String aclUuid;
            private String redirectRule;
            private String serverGroupUuid;

            public String getRedirectRuleUuid() {
                return redirectRuleUuid;
            }

            public void setRedirectRuleUuid(String redirectRuleUuid) {
                this.redirectRuleUuid = redirectRuleUuid;
            }

            public String getAclUuid() {
                return aclUuid;
            }

            public void setAclUuid(String aclUuid) {
                this.aclUuid = aclUuid;
            }

            public String getRedirectRule() {
                return redirectRule;
            }

            public void setRedirectRule(String redirectRule) {
                this.redirectRule = redirectRule;
            }

            public String getServerGroupUuid() {
                return serverGroupUuid;
            }

            public void setServerGroupUuid(String serverGroupUuid) {
                this.serverGroupUuid = serverGroupUuid;
            }
        }

        public String getListenerUuid() {
            return listenerUuid;
        }

        public void setListenerUuid(String listenerUuid) {
            this.listenerUuid = listenerUuid;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public void setParameters(List<String> parameters) {
            this.parameters = parameters;
        }

        public String getLbUuid() {
            return lbUuid;
        }

        public void setLbUuid(String lbUuid) {
            this.lbUuid = lbUuid;
        }

        public String getVip() {
            return vip;
        }

        public void setVip(String vip) {
            this.vip = vip;
        }

        public List<String> getNicIps() {
            return nicIps;
        }

        public void setNicIps(List<String> nicIps) {
            this.nicIps = nicIps;
        }

        public int getInstancePort() {
            return instancePort;
        }

        public void setInstancePort(int instancePort) {
            this.instancePort = instancePort;
        }

        public int getLoadBalancerPort() {
            return loadBalancerPort;
        }

        public void setLoadBalancerPort(int loadBalancerPort) {
            this.loadBalancerPort = loadBalancerPort;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getCertificateUuid() {
            return certificateUuid;
        }

        public void setCertificateUuid(String certificateUuid) {
            this.certificateUuid = certificateUuid;
        }

        public String getPublicNic() {
            return publicNic;
        }

        public void setPublicNic(String publicNic) {
            this.publicNic = publicNic;
        }

        public String getSecurityPolicyType() {
            return securityPolicyType;
        }

        public void setSecurityPolicyType(String securityPolicyType) {
            this.securityPolicyType = securityPolicyType;
        }

        public List<ServerGroup> getServerGroups() {
            return serverGroups;
        }

        public void setServerGroups(List<ServerGroup> serverGroups) {
            this.serverGroups = serverGroups;
        }

        public List<RedirectRule> getRedirectRules() {
            return redirectRules;
        }

        public void setRedirectRules(List<RedirectRule> redirectRules) {
            this.redirectRules = redirectRules;
        }
    }

    public static class RefreshLbCmd extends AgentCommand {
        List<LbTO> lbs;
        public Boolean  enableHaproxyLog;

        public List<LbTO> getLbs() {
            return lbs;
        }

        public void setLbs(List<LbTO> lbs) {
            this.lbs = lbs;
        }
    }

    public static class RefreshLbLogLevelCmd extends AgentCommand {
        String level;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    public static class RefreshLbLogLevelRsp extends AgentResponse {
    }

    public static class RefreshLbRsp extends AgentResponse {
    }

    public static class CertificateCmd extends AgentCommand {
        String uuid;
        String certificate;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }
    }

    public static class CertificateRsp extends AgentResponse {
    }

    public static class DeleteLbCmd extends AgentCommand {
        List<LbTO> lbs;

        public List<LbTO> getLbs() {
            return lbs;
        }

        public void setLbs(List<LbTO> lbs) {
            this.lbs = lbs;
        }
    }

    public static class DeleteLbRsp extends AgentResponse {
    }

    public static final String REFRESH_LB_PATH = "/lb/refresh";
    public static final String DELETE_LB_PATH = "/lb/delete";
    public static final String REFRESH_LB_LOG_LEVEL_PATH = "/lb/log/level";
    public static final String CREATE_CERTIFICATE_PATH = "/certificate/create";
    public static final String DELETE_CERTIFICATE_PATH = "/certificate/delete";

    private List<LbTO> makeLbTOs(final LoadBalancerStruct struct, VirtualRouterVmInventory vr) {
        VipInventory vip = struct.getVip();
        VmNicInventory publicNic = vr.getVmNics().stream()
                .filter(n -> n.getL3NetworkUuid().equals(vip.getL3NetworkUuid()))
                .findFirst()
                .orElse(null);
        if (publicNic == null) {
            return new ArrayList<>();
        }

        return transformAndRemoveNull(struct.getListeners(), it -> new MakeLbTOContext()
                .withStruct(struct)
                .withPublicNic(publicNic)
                .withVip(vip)
                .create(it));
    }

    private static class MakeLbTOContext {
        LoadBalancerStruct struct;
        VmNicInventory publicNic;
        VipInventory vip;

        MakeLbTOContext withStruct(LoadBalancerStruct struct) {
            this.struct = struct;
            return this;
        }

        MakeLbTOContext withPublicNic(VmNicInventory publicNic) {
            this.publicNic = publicNic;
            return this;
        }

        MakeLbTOContext withVip(VipInventory vip) {
            this.vip = vip;
            return this;
        }

        private List<String> makeAcl(LoadBalancerListenerInventory listenerInv) {
            String  aclEntry = "";
            List<String> aclRules = new ArrayList<>();
            List<LoadBalancerListenerACLRefInventory> refs = listenerInv.getAclRefs();
            if (refs.isEmpty()) {
                aclRules.add(String.format("aclEntry::%s", aclEntry));
                return aclRules;
            }

            List<LoadBalancerListenerACLRefInventory> aclRefInventories = refs.stream().filter(ref -> !ref.getType().equals(LoadBalancerAclType.redirect.toString())).collect(Collectors.toList());
            if (aclRefInventories.isEmpty()) {
                return aclRules;
            }

            aclRules.add(String.format("aclType::%s", aclRefInventories.get(0).getType()));

            List<String> aclUuids = aclRefInventories.stream().map(LoadBalancerListenerACLRefInventory::getAclUuid).collect(Collectors.toList());
            List<String> entry = Q.New(AccessControlListEntryVO.class).select(AccessControlListEntryVO_.ipEntries)
                                  .in(AccessControlListEntryVO_.aclUuid, aclUuids).listValues();
            if (!entry.isEmpty()) {
                aclEntry = StringUtils.join(entry.toArray(), ',');
            }
            aclRules.add(String.format("aclEntry::%s", aclEntry));
            return aclRules;
        }

        private List<AccessControlListEntryVO> sortAclEntry(List<AccessControlListEntryVO> entries) {
            if (entries == null || entries.size() <= 1) {
                return entries;
            }
            entries.sort((entry1, entry2) -> {
                int i = entry2.getDomain().length() - entry1.getDomain().length();
                if (i != 0) {
                    return i;
                }
                int urlLength1 = entry1.getUrl() == null ? 0 : entry1.getUrl().length();
                int urlLength2 = entry2.getUrl() == null ? 0 : entry2.getUrl().length();
                return urlLength2 - urlLength1;
            });
            return entries;
        }

        private List<LbTO.RedirectRule> makeRedirectAcl(LoadBalancerListenerInventory listenerInv, LbTO lbTO) {
            ArrayList<LbTO.RedirectRule> redirectRules = new ArrayList<>();

            if (lbTO.getServerGroups() == null || lbTO.getServerGroups().isEmpty()) {
                return redirectRules;
            }

            List<LoadBalancerListenerACLRefInventory> refs = listenerInv.getAclRefs();
            if (refs.isEmpty()) {
                return redirectRules;
            }

            List<String> aclUuids = refs.stream().filter(ref -> ref.getType().equals(LoadBalancerAclType.redirect.toString())).map(LoadBalancerListenerACLRefInventory::getAclUuid).collect(Collectors.toList());
            if (aclUuids.isEmpty()) {
                return redirectRules;
            }
            List<AccessControlListEntryVO> entries = Q.New(AccessControlListEntryVO.class).in(AccessControlListEntryVO_.aclUuid, aclUuids).list();
            List<String> usedSgUuids = new ArrayList<>();
            List<String> usedAggSgUuids = new ArrayList<>();
            //sort acl entry:  The more accurate, the more forward
            //accurate forward, longer forward
            List<AccessControlListEntryVO> domainWildWordMatchEntries = new ArrayList<>();
            List<AccessControlListEntryVO> domainAccurateMatchEntries = new ArrayList<>();
            List<AccessControlListEntryVO> onlyUrlEntries = new ArrayList<>();
            List<AccessControlListEntryVO> afterSortEntries = new ArrayList<>();
            for (AccessControlListEntryVO entryVO : entries) {
                if (entryVO.getMatchMethod().equals("Url")) {
                    onlyUrlEntries.add(entryVO);
                } else if ("AccurateMatch".equals(entryVO.getCriterion())) {
                    domainAccurateMatchEntries.add(entryVO);
                } else {
                    domainWildWordMatchEntries.add(entryVO);
                }
            }
            sortAclEntry(domainAccurateMatchEntries);
            sortAclEntry(domainWildWordMatchEntries);
            afterSortEntries.addAll(domainAccurateMatchEntries);
            afterSortEntries.addAll(domainWildWordMatchEntries);
            afterSortEntries.addAll(onlyUrlEntries);

            List<String> sgOwnBackenServerUuids = lbTO.getServerGroups().stream().map(LbTO.ServerGroup::getServerGroupUuid).collect(Collectors.toList());
            for (AccessControlListEntryVO entry : afterSortEntries) {
                List<String> serverGroupUuids = refs.stream()
                        .filter(ref -> ref.getListenerUuid().equals(listenerInv.getUuid()) && ref.getAclUuid().equals(entry.getAclUuid()) && ref.getServerGroupUuid() != null && sgOwnBackenServerUuids.contains(ref.getServerGroupUuid()))
                        .map(LoadBalancerListenerACLRefInventory::getServerGroupUuid).sorted().collect(Collectors.toList());
                if (serverGroupUuids.isEmpty()) {
                    continue;
                }

                LbTO.RedirectRule redirectRule = new LbTO.RedirectRule();
                if (serverGroupUuids.size() == 1) {
                    usedSgUuids.addAll(serverGroupUuids);
                    redirectRule.setAclUuid(entry.getAclUuid());
                    redirectRule.setRedirectRuleUuid(entry.getUuid());
                    redirectRule.setServerGroupUuid(serverGroupUuids.get(0));
                    redirectRule.setRedirectRule(entry.getRedirectRule());
                } else {
                    usedAggSgUuids.addAll(serverGroupUuids);
                    redirectRule.setAclUuid(entry.getAclUuid());
                    redirectRule.setRedirectRuleUuid(entry.getUuid());
                    redirectRule.setRedirectRule(entry.getRedirectRule());

                    StringBuilder stringBuilder = new StringBuilder();
                    for (String sgUuid : serverGroupUuids) {
                        stringBuilder.append(sgUuid);
                    }
                    String polymerizedUuid = DigestUtils.md5Hex(stringBuilder.toString());

                    boolean needAddServerGroup = true;
                    if (!lbTO.getServerGroups().isEmpty()) {
                        needAddServerGroup = lbTO.getServerGroups().stream().noneMatch(sg -> sg.getServerGroupUuid().equals(polymerizedUuid));
                    }

                    if (needAddServerGroup) {
                        LbTO.ServerGroup serverGroup = new LbTO.ServerGroup();
                        serverGroup.setBackendServers(new ArrayList<>());
                        for (String sgUuid : serverGroupUuids) {
                            for (LbTO.ServerGroup serverGroup1 : lbTO.serverGroups) {
                                if (sgUuid.equals(serverGroup1.serverGroupUuid)) {
                                    serverGroup.getBackendServers().addAll(serverGroup1.getBackendServers());
                                }
                            }
                        }
                        serverGroup.setName(polymerizedUuid);
                        serverGroup.setServerGroupUuid(polymerizedUuid);
                        lbTO.getServerGroups().add(serverGroup);
                    }
                    redirectRule.setServerGroupUuid(polymerizedUuid);
                    usedSgUuids.add(polymerizedUuid);
                }
                redirectRules.add(redirectRule);
            }

            ArrayList<LbTO.BackendServer> backendServers = new ArrayList<>();
            Iterator<LbTO.ServerGroup> iterator = lbTO.getServerGroups().iterator();
            if (!usedAggSgUuids.isEmpty()) {
                if (!usedSgUuids.isEmpty()) {
                    usedAggSgUuids = usedAggSgUuids.stream().filter(sgUuid -> !usedSgUuids.contains(sgUuid)).collect(Collectors.toList());
                }
            }
            while (iterator.hasNext()) {
                LbTO.ServerGroup sg = iterator.next();
                if (!usedSgUuids.contains(sg.getServerGroupUuid()) && !usedAggSgUuids.contains(sg.getServerGroupUuid())) {
                    backendServers.addAll(sg.getBackendServers());
                    iterator.remove();
                }
                if (usedAggSgUuids.contains(sg.getServerGroupUuid())) {
                    iterator.remove();
                }
            }
            if (!backendServers.isEmpty()) {
                if (!redirectRules.isEmpty()) {
                    LbTO.ServerGroup serverGroup = new LbTO.ServerGroup();
                    serverGroup.setBackendServers(backendServers);
                    serverGroup.setName("default-server-group");
                    serverGroup.setDefault(true);
                    serverGroup.setServerGroupUuid("defaultServerGroup");
                    lbTO.getServerGroups().add(serverGroup);
                }
            }

            boolean isDefaultPort = false;  // http 80;https 443
            ArrayList<LbTO.RedirectRule> formatRedirectRules = new ArrayList<>();
            if ( (lbTO.getMode().equals(LoadBalancerConstants.LB_PROTOCOL_HTTP) && lbTO.getLoadBalancerPort() == LoadBalancerConstants.PROTOCOL_HTTP_DEFAULT_PORT ) || (lbTO.getMode().equals(LoadBalancerConstants.LB_PROTOCOL_HTTPS) && lbTO.getLoadBalancerPort() == LoadBalancerConstants.PROTOCOL_HTTPS_DEFAULT_PORT) ){
                formatRedirectRules.addAll(redirectRules);
                isDefaultPort = true;
            }

            for(LbTO.RedirectRule rule:redirectRules){
                LbTO.RedirectRule formatRule = new LbTO.RedirectRule();
                formatRule.setRedirectRule(rule.getRedirectRule());
                formatRule.setRedirectRuleUuid(rule.getRedirectRuleUuid());
                formatRule.setAclUuid(rule.getAclUuid());
                formatRule.setServerGroupUuid(rule.getServerGroupUuid());

                String matchMethod = Q.New(AccessControlListEntryVO.class).select(AccessControlListEntryVO_.matchMethod).eq(AccessControlListEntryVO_.uuid, rule.getRedirectRuleUuid()).findValue();
                boolean isSkipInsertPort = ( LoadBalancerConstants.MatchMethod.Domain.toString().equals(matchMethod) || LoadBalancerConstants.MatchMethod.Url.toString().equals(matchMethod) );
                if ( isSkipInsertPort && isDefaultPort ){
                    continue;
                } else if( isSkipInsertPort){
                    formatRedirectRules.add(formatRule);
                } else{
                    insertPortToRedirectRule(formatRule, lbTO);
                    formatRedirectRules.add(formatRule);
                }
            }
            return formatRedirectRules;
        }

        private void insertPortToRedirectRule(LbTO.RedirectRule redirectRule,LbTO lbTO){
            //add lbport after domain name
            StringBuffer rule = new StringBuffer(redirectRule.getRedirectRule());
            String insertRule = ":" + lbTO.getLoadBalancerPort();
            int index = rule.indexOf("/");
            if (index != -1) {
                rule.insert(index,insertRule);
            }
            redirectRule.setRedirectRule(rule.toString());
        }

        public LbTO create(LoadBalancerListenerInventory l) {
            LbTO to = new LbTO();
            to.setInstancePort(l.getInstancePort());
            to.setLoadBalancerPort(l.getLoadBalancerPort());
            to.setLbUuid(l.getLoadBalancerUuid());
            to.setListenerUuid(l.getUuid());
            to.setMode(l.getProtocol());
            to.setVip(vip.getIp());
            to.setSecurityPolicyType(l.getSecurityPolicyType());
            if (l.getCertificateRefs() != null && !l.getCertificateRefs().isEmpty()) {
                to.setCertificateUuid(l.getCertificateRefs().get(0).getCertificateUuid());
            }

            List<LoadBalancerServerGroupInventory> groupInvs = struct.getListenerServerGroupMap().get(l.getUuid());
            List<String> params = new ArrayList<>();
            List<String> ips = new ArrayList<>();
            List<LbTO.ServerGroup> serverGroups = new ArrayList<>();
            if (groupInvs != null) {
                for (LoadBalancerServerGroupInventory groupInv : groupInvs) {
                    LbTO.ServerGroup serverGroup = new LbTO.ServerGroup();
                    serverGroup.setName(groupInv.getName());
                    serverGroup.setServerGroupUuid(groupInv.getUuid());
                    List<LbTO.BackendServer> backendServers = new ArrayList<>();
                    serverGroup.setBackendServers(backendServers);
                    List<LoadBalancerServerGroupVmNicRefInventory> nicRefInventories = Optional.ofNullable(groupInv.getVmNicRefs()).orElse(new ArrayList<>()).stream()
                            .sorted((r1, r2) -> (int) (r1.getId() - r2.getId()))
                            .collect(Collectors.toList());
                    List<LoadBalancerServerGroupServerIpInventory> ipRefInventories = Optional.ofNullable(groupInv.getServerIps()).orElse(new ArrayList<>()).stream()
                            .sorted((r1, r2) -> (int) (r1.getId() - r2.getId()))
                            .collect(Collectors.toList());
                    for (LoadBalancerServerGroupVmNicRefInventory nicRef : nicRefInventories) {
                        if (nicRef.getStatus().equals(LoadBalancerVmNicStatus.Inactive.toString())) {
                            continue;
                        }

                        VmNicInventory nic = struct.getVmNics().get(nicRef.getVmNicUuid());
                        if (nic == null) {
                            throw new CloudRuntimeException(String.format("cannot find nic[uuid:%s]", nicRef.getVmNicUuid()));
                        }
                        if(nic.getIp() == null || nic.getIp().isEmpty()){
                            continue;
                        }
                        ips.add(nic.getIp());
                        params.add(String.format("balancerWeight::%s::%s", nic.getIp(), nicRef.getWeight()));
                        backendServers.add(new LbTO.BackendServer(nic.getIp(), nicRef.getWeight()));
                    }

                    for (LoadBalancerServerGroupServerIpInventory ipRef : ipRefInventories) {
                        if (ipRef.getStatus().equals(LoadBalancerBackendServerStatus.Inactive.toString())) {
                            continue;
                        }

                        if(ipRef.getIpAddress() == null || ipRef.getIpAddress().isEmpty()){
                            continue;
                        }
                        ips.add(ipRef.getIpAddress());
                        params.add(String.format("balancerWeight::%s::%s",ipRef.getIpAddress(), ipRef.getWeight()));
                        backendServers.add(new LbTO.BackendServer(ipRef.getIpAddress(), ipRef.getWeight()));
                    }

                    if (!backendServers.isEmpty()) {
                        serverGroups.add(serverGroup);
                    }
                }
            }
            to.setNicIps(ips.stream().sorted().collect(Collectors.toList()));
            to.setPublicNic(publicNic.getMac());
            to.setServerGroups(serverGroups);
            // vnicUuid::weight
            params.addAll(transformAndRemoveNull(struct.getTags().get(l.getUuid()), arg -> {
                if (LoadBalancerSystemTags.BALANCER_WEIGHT.isMatch(arg)) {
                    /*
                        4.0 lb server ip weight configuration from nicRefVO and serverIpVO,not systemTag
                     */
                    return null;
                }
                return arg;
            }));
            to.setRedirectRules(makeRedirectAcl(l, to));
            params.addAll(makeAcl(l));
            to.setParameters(params);
            return to;
        }
    }

    private List<String> getCertificates(List<LoadBalancerStruct> structs) {
        List<String> certificateUuids = new ArrayList<>();
        for (LoadBalancerStruct struct : structs) {
            for (LoadBalancerListenerInventory listenerInv : struct.getListeners()) {
                if (listenerInv.getCertificateRefs() == null || listenerInv.getCertificateRefs().isEmpty()) {
                    continue;
                }

                List<LoadBalancerServerGroupInventory> serverGroups = struct.getListenerServerGroupMap().get(listenerInv.getUuid());
                if (serverGroups == null) {
                    continue;
                }

                List<String> nics = new ArrayList<>();
                List<String> serverIps = new ArrayList<>();
                for (LoadBalancerServerGroupInventory group : serverGroups) {
                    nics.addAll(group.getVmNicRefs().stream().map(LoadBalancerServerGroupVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
                    serverIps.addAll(group.getServerIps().stream().map(LoadBalancerServerGroupServerIpInventory::getIpAddress).collect(Collectors.toList()));
                }
                if (nics.isEmpty() && serverIps.isEmpty()) {
                    continue;
                }

                if (!certificateUuids.contains(listenerInv.getCertificateRefs().get(0).getCertificateUuid())) {
                    certificateUuids.add(listenerInv.getCertificateRefs().get(0).getCertificateUuid());
                }
            }
        }

        return certificateUuids;
    }

    private void refreshCertificate(VirtualRouterVmInventory vr, boolean checkVrState, List<LoadBalancerStruct> struct, final Completion completion){
        List<String> certificateUuids = getCertificates(struct);

        List<ErrorCode> errors = new ArrayList<>();
        new While<>(certificateUuids).each((uuid, wcmpl) -> {
            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
            msg.setVmInstanceUuid(vr.getUuid());
            msg.setPath(CREATE_CERTIFICATE_PATH);
            msg.setCheckStatus(checkVrState);

            CertificateCmd cmd = new CertificateCmd();
            CertificateVO vo = dbf.findByUuid(uuid, CertificateVO.class);
            cmd.setUuid(uuid);
            cmd.setCertificate(vo.getCertificate());

            msg.setCommand(cmd);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(msg, new CloudBusCallBack(wcmpl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        CertificateRsp rsp = ((VirtualRouterAsyncHttpCallReply) reply).toResponse(CertificateRsp.class);
                        if (rsp.isSuccess()) {
                            wcmpl.done();
                        } else {
                            errors.add(operr("operation error, because:%s", rsp.getError()));
                            wcmpl.allDone();
                        }
                    } else {
                        errors.add(reply.getError());
                        wcmpl.allDone();
                    }
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errors.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errors.get(0));
                }
            }
        });
    }

    private void rollbackCertificate(VirtualRouterVmInventory vr, boolean checkVrState, List<LoadBalancerStruct> struct, final NoErrorCompletion completion){
        List<String> certificateUuids = getCertificates(struct);

        new While<>(certificateUuids).each((uuid, wcmpl) -> {
            VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
            msg.setVmInstanceUuid(vr.getUuid());
            msg.setPath(DELETE_CERTIFICATE_PATH);
            msg.setCheckStatus(checkVrState);

            CertificateCmd cmd = new CertificateCmd();
            cmd.setUuid(uuid);

            msg.setCommand(cmd);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
            bus.send(msg, new CloudBusCallBack(wcmpl) {
                @Override
                public void run(MessageReply reply) {
                    wcmpl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    private void refreshLbToVirtualRouter(VirtualRouterVmInventory vr, LoadBalancerStruct struct, Completion completion) {
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(REFRESH_LB_PATH);
        msg.setCheckStatus(true);

        RefreshLbCmd cmd = new RefreshLbCmd();
        cmd.lbs = makeLbTOs(struct, vr);
        if (cmd.lbs.isEmpty()) {
            completion.success();
            return;
        }
        cmd.enableHaproxyLog = rcf.getResourceConfigValue(VyosGlobalConfig.ENABLE_HAPROXY_LOG, vr.getUuid(), Boolean.class);

        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    RefreshLbRsp rsp = ((VirtualRouterAsyncHttpCallReply) reply).toResponse(RefreshLbRsp.class);
                    if (rsp.isSuccess()) {
                        new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                        completion.success();
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    public void refresh(VirtualRouterVmInventory vr, LoadBalancerStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("refresh-lb-to-virtualRouter");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "refresh-lb-ceriticae-to-virtualRouter";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshCertificate(vr, true, Collections.singletonList(struct), new Completion(trigger) {
                            @Override
                            public void success() {
                                refreshCertificateOnHaRouter(vr.getUuid(), Collections.singletonList(struct), new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        rollbackCertificate(vr, true, Collections.singletonList(struct), new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                rollbackCertificateOnHaRouter(vr.getUuid(), Collections.singletonList(struct), new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.rollback();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.rollback();
                                    }
                                });
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-listener-to-virtualRouter";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshLbToVirtualRouter(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                refreshLbToVirtualRouterHa(vr, struct, new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        trigger.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                    }
                                });
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void stopVip(final LoadBalancerStruct struct, final List<VmNicInventory> nics, final Completion completion) {
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(struct.getLb().getType());
        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
        vipStruct.setUseFor(f.getNetworkServiceType());
        vipStruct.setServiceUuid(struct.getLb().getUuid());

        Set<String> guestL3NetworkUuids = nics.stream()
                                              .map(VmNicInventory::getL3NetworkUuid)
                                              .collect(Collectors.toSet());

        /*remove the l3networks still attached*/
        Set<String> vnicUuidsAttached = new HashSet<>();
        for (LoadBalancerListenerInventory listener : struct.getListeners()) {
            for (LoadBalancerServerGroupInventory group : struct.getListenerServerGroupMap().get(listener.getUuid())) {
                vnicUuidsAttached.addAll(group.getVmNicRefs().stream().map(LoadBalancerServerGroupVmNicRefInventory::getVmNicUuid).collect(Collectors.toList()));
            }
        }
        if (!vnicUuidsAttached.isEmpty()) {
            List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, vnicUuidsAttached).listValues();
            if (l3Uuids != null && !l3Uuids.isEmpty()) {
                guestL3NetworkUuids.removeAll(l3Uuids);
            }
        }

        if (guestL3NetworkUuids.isEmpty()) {
            completion.success();
            return;
        }
        vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
        vipStruct.setServiceProvider(getLoadLancerServiceProvider(vipStruct.getPeerL3NetworkUuids()));
        Vip v = new Vip(struct.getLb().getVipUuid());
        v.setStruct(vipStruct);
        v.stop(completion);
    }

    private void acquireVip(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, final List<VmNicInventory> nics, final Completion completion) {
        LoadBalancerVO loadBalancerVO = dbf.findByUuid(struct.getLb().getUuid(), LoadBalancerVO.class);
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(loadBalancerVO.getType().toString());

        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
        vipStruct.setUseFor(f.getNetworkServiceType());
        vipStruct.setServiceUuid(struct.getLb().getUuid());
        Set<String> guestL3NetworkUuids = nics.stream()
                .map(VmNicInventory::getL3NetworkUuid)
                .collect(Collectors.toSet());

        vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
        vipStruct.setServiceProvider(getLoadLancerServiceProvider(vipStruct.getPeerL3NetworkUuids()));
        Vip v = new Vip(struct.getLb().getVipUuid());
        v.setStruct(vipStruct);
        v.acquire(new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void startVrIfNeededAndRefresh(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, List<VmNicInventory> nics, final Completion completion) {
        acquireVip(vr, struct, nics, new Completion(completion) {
            @Override
            public void success() {
                startVrIfNeededAndRefresh(vr, struct, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void startVrIfNeededAndRefresh(final VirtualRouterVmInventory vr, final LoadBalancerStruct struct, final Completion completion) {
        if (!VmInstanceState.Stopped.toString().equals(vr.getState())) {
            refresh(vr, struct, completion);
            return;
        }

        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));

        LoadBalancerVO loadBalancerVO = dbf.findByUuid(struct.getLb().getUuid(), LoadBalancerVO.class);
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(loadBalancerVO.getType().toString());

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("start-vr-%s-and-refresh-lb-%s", vr.getUuid(), struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "start-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        StartVmInstanceMsg msg = new StartVmInstanceMsg();
                        msg.setVmInstanceUuid(vr.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-vip-on-vr";
                    boolean success = false;

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(f.getNetworkServiceType());
                        vipStruct.setServiceUuid(struct.getLb().getUuid());
                        vipStruct.setServiceProvider(getLoadLancerServiceProvider(vr.getGuestL3Networks()));
                        vipStruct.setPeerL3NetworkUuids(vr.getGuestL3Networks());

                        Vip v = new Vip(struct.getLb().getVipUuid());
                        v.setStruct(vipStruct);
                        v.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                success = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (!success) {
                            trigger.rollback();
                            return;
                        }

                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(f.getNetworkServiceType());
                        vipStruct.setServiceUuid(struct.getLb().getUuid());
                        vipStruct.setPeerL3NetworkUuids(vr.getGuestL3Networks());
                        vipStruct.setServiceProvider(getLoadLancerServiceProvider(vr.getGuestL3Networks()));
                        Vip v = new Vip(vip.getUuid());
                        v.setStruct(vipStruct);
                        v.stop(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void addVmNics(final LoadBalancerStruct struct, List<VmNicInventory> nics, final Completion completion) {
        if (struct.getLb().getType().equals(LoadBalancerType.Shared.toString()) && nics.isEmpty()) {
            completion.fail(operr("vmnic must be specified for share loadbalancer"));
            return;
        }

        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid(),
                nics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList()));
        if (vr != null) {
            startVrIfNeededAndRefresh(vr, struct, nics, completion);
            return;
        }

        L3NetworkInventory nicL3 = null;
        if (!nics.isEmpty()) {
            nicL3 = L3NetworkInventory.valueOf(dbf.findByUuid(nics.get(0).getL3NetworkUuid(), L3NetworkVO.class));
        }
        final L3NetworkInventory l3 = nicL3;
        final VipInventory vip = VipInventory.valueOf(dbf.findByUuid(struct.getLb().getVipUuid(), VipVO.class));
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, struct.getLb().getVipUuid()).listValues();
        VipUseForList useForList = new VipUseForList(useFor);
        if (!useForList.isIncluded(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)) {
            logger.warn(String.format("the vip[uuid:%s, name:%s, ip:%s, useFor: %s] is not for load balancer", vip.getUuid(),
                            vip.getName(), vip.getIp(), vip.getUseFor()));
        }

        final boolean separateVr = LoadBalancerSystemTags.SEPARATE_VR.hasTag(struct.getLb().getUuid());

        LoadBalancerVO loadBalancerVO = dbf.findByUuid(struct.getLb().getUuid(), LoadBalancerVO.class);
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(loadBalancerVO.getType().toString());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-nic-to-vr-lb-%s", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            VirtualRouterVmInventory vr;

            @Override
            public void setup() {
                if (separateVr) {
                    flow(new Flow() {
                        String __name__ = "lock-vip";

                        @Override
                        public boolean skip(Map data) {
                            return nics.isEmpty();
                        }

                        /* now the vip support multi services and it doesn't need to lock vip that will be locked
                           by itself in vip module via to VipNetworkServicesRefVO * */
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(f.getNetworkServiceType());
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            Set<String> guestL3NetworkUuids = nics.stream()
                                                                  .map(VmNicInventory::getL3NetworkUuid)
                                                                  .collect(Collectors.toSet());

                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.acquire(new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            List<String> attachedVmNicUuids = struct.getActiveVmNics();
                            Set<String> guestL3NetworkUuids = nics.stream()
                                                                  .map(VmNicInventory::getL3NetworkUuid)
                                                                  .collect(Collectors.toSet());
                            guestL3NetworkUuids.removeAll(attachedVmNicUuids);
                            if (guestL3NetworkUuids.isEmpty()) {
                                logger.debug(String.format("there are vmnics[uuids:%s] attached on loadbalancer[uuid:%s], " +
                                        "wont release vip[uuid: %s]", attachedVmNicUuids, struct.getLb().getUuid(), vip.getUuid()));
                                trigger.rollback();
                                return;
                            }
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(f.getNetworkServiceType());
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
                            vipStruct.setServiceProvider(getLoadLancerServiceProvider(vipStruct.getPeerL3NetworkUuids()));
                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.stop(new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "create-separate-vr";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            VirtualRouterStruct s = new VirtualRouterStruct();
                            s.setInherentSystemTags(list(VirtualRouterSystemTags.DEDICATED_ROLE_VR.getTagFormat(), VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat()));
                            s.setVirtualRouterVmSelector(new VirtualRouterVmSelector() {
                                @Override
                                public VirtualRouterVmVO select(List<VirtualRouterVmVO> vrs) {
                                    return null;
                                }
                            });
                            s.setL3Network(l3);
                            s.setNotGatewayForGuestL3Network(true);

                            acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger) {
                                @Override
                                public void success(VirtualRouterVmInventory returnValue) {
                                    vr = returnValue;
                                    new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (vr == null) {
                                trigger.rollback();
                                return;
                            }

                            DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        //TODO:
                                        logger.warn(String.format("failed to destroy vr[uuid:%s], %s. Need a cleanup", vr.getUuid(), reply.getError()));
                                    }

                                    trigger.rollback();
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "create-vip-on-vr";
                        boolean success = false;

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            vipVrBkd.acquireVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                                @Override
                                public void success() {
                                    success = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (!success) {
                                trigger.rollback();
                                return;
                            }

                            vipVrBkd.releaseVipOnVirtualRouterVm(vr, vip, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });
                        }
                    });

                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "acquire-vr";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            VirtualRouterStruct s = new VirtualRouterStruct(l3);
                            s.setLoadBalancerUuid(struct.getLb().getUuid());
                            acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger) {
                                @Override
                                public void success(VirtualRouterVmInventory returnValue) {
                                    vr = returnValue;
                                    new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new Flow() {
                        String __name__ = "acquire-vip";
                        boolean success = false;

                        @Override
                        public boolean skip(Map data) {
                            return nics.isEmpty();
                        }

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(f.getNetworkServiceType());
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            Set<String> guestL3NetworkUuids = nics.stream()
                                    .map(VmNicInventory::getL3NetworkUuid)
                                    .collect(Collectors.toSet());
                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
                            vipStruct.setServiceProvider(getLoadLancerServiceProvider(vipStruct.getPeerL3NetworkUuids()));

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.acquire(new Completion(trigger) {
                                @Override
                                public void success() {
                                    success = true;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void rollback(final FlowRollback trigger, Map data) {
                            if (!success) {
                                trigger.rollback();
                                return;
                            }

                            List<String> attachedVmNicUuids = struct.getActiveVmNics();
                            Set<String> guestL3NetworkUuids = nics.stream()
                                                                  .map(VmNicInventory::getL3NetworkUuid)
                                                                  .collect(Collectors.toSet());
                            guestL3NetworkUuids.removeAll(attachedVmNicUuids);
                            if (guestL3NetworkUuids.isEmpty()) {
                                logger.debug(String.format("there are vmnics[uuids:%s] attached on loadbalancer[uuid:%s], " +
                                        "wont release vip[uuid: %s]", attachedVmNicUuids, struct.getLb().getUuid(), vip.getUuid()));
                                trigger.rollback();
                                return;
                            }
                            
                            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                            vipStruct.setUseFor(f.getNetworkServiceType());
                            vipStruct.setServiceUuid(struct.getLb().getUuid());
                            vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
                            vipStruct.setServiceProvider(getLoadLancerServiceProvider(vipStruct.getPeerL3NetworkUuids()));

                            Vip v = new Vip(vip.getUuid());
                            v.setStruct(vipStruct);
                            v.stop(new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.rollback();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    logger.warn(String.format("failed to release vip[uuid:%s, ip:%s] on vr[uuid:%s], continue to rollback",
                                            vip.getUuid(), vip.getIp(), vr.getUuid()));
                                    trigger.rollback();
                                }
                            });

                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-on-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        proxy.attachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void addVmNic(final LoadBalancerStruct struct, VmNicInventory nic, final Completion completion) {
        addVmNics(struct, list(nic), completion);
    }

    @Override
    public void removeVmNic(LoadBalancerStruct struct, VmNicInventory nic, Completion completion) {
        removeVmNics(struct, list(nic), completion);
    }

    @Override
    public void removeVmNics(LoadBalancerStruct struct, List<VmNicInventory> nics, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());

        final boolean separateVr = LoadBalancerSystemTags.SEPARATE_VR.hasTag(struct.getLb().getUuid());
        if ( separateVr ) {
            logger.error("not support the separate vrouter currently.");
            // no support the case
            completion.success();
            return;
        }

        if (vr == null) {
            // the vr has been destroyed, it just need modify the Vip
            if (!nics.isEmpty()) {
                stopVip(struct, nics, completion);
            }
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-nic-from-vr-lb-%s", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            //VirtualRouterVmInventory vr;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-on-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
                            // no need to remove as the vr is stopped
                            trigger.next();
                            return;
                        }

                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-l3network-from-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (nics.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        stopVip(struct, nics, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        if (struct.getListeners().isEmpty() || struct.getAllVmNics().isEmpty()) {
                            proxy.detachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                        }

                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void addListener(LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            throw new OperationFailureException(operr("cannot find virtual router for load balancer [uuid:%s]", struct.getLb().getUuid()));
        }

        startVrIfNeededAndRefresh(vr, struct, completion);
    }

    @Override
    public void removeListener(final LoadBalancerStruct struct, LoadBalancerListenerInventory listener, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        final boolean separateVr = LoadBalancerSystemTags.SEPARATE_VR.hasTag(struct.getLb().getUuid());
        if ( separateVr ) {
            logger.error("not support the separate vrouter currently.");
            completion.success();
            return;
        }

        LoadBalancerVO loadBalancerVO = dbf.findByUuid(struct.getLb().getUuid(), LoadBalancerVO.class);
        LoadBalancerFactory f = lbMgr.getLoadBalancerFactory(loadBalancerVO.getType().toString());
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-Listener-from-vr-lb-%s", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "refresh-lb-on-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (VmInstanceState.Stopped.toString().equals(vr.getState())) {
                            trigger.next();
                            return;
                        }

                        refresh(vr, struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-l3networks-from-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(f.getNetworkServiceType());
                        vipStruct.setServiceUuid(struct.getLb().getUuid());

                        List<String> nicUuids = struct.getAllVmNicsOfListener(listener);
                        if (nicUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        List<String> guestL3NetworkUuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, nicUuids).listValues();

                        /*remove the l3networks still attached*/
                        List<String> vnicUuidsAttached = struct.getAllVmNics();
                        if (!vnicUuidsAttached.isEmpty()) {
                            List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).in(VmNicVO_.uuid, vnicUuidsAttached).listValues();
                            if (l3Uuids != null && !l3Uuids.isEmpty()) {
                                guestL3NetworkUuids.removeAll(l3Uuids);
                            }
                        }
                        if (guestL3NetworkUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        vipStruct.setPeerL3NetworkUuids(new ArrayList<>(guestL3NetworkUuids));
                        vipStruct.setServiceProvider(getLoadLancerServiceProvider(vipStruct.getPeerL3NetworkUuids()));
                        Vip v = new Vip(struct.getLb().getVipUuid());
                        v.setStruct(vipStruct);
                        v.stop(new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        if (struct.getListeners().isEmpty() || struct.getAllVmNics().isEmpty()) {
                            proxy.detachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                        }
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void destroyLoadBalancer(final LoadBalancerStruct struct, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-lb-%s-from-vr", struct.getLb().getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-from-vr";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
                        if (vr == null) {
                            // the vr has been destroyed
                            trigger.next();
                            return;
                        }

                        List<String> roles = new VirtualRouterRoleManager().getAllRoles(vr.getUuid());
                        if (roles.size() == 1 && roles.contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat())) {
                            DestroyVmInstanceMsg msg = new DestroyVmInstanceMsg();
                            msg.setVmInstanceUuid(vr.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                            }
                                }
                            });
                        } else if (roles.size() > 1 && roles.contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat())) {
                            destroyLoadBalancerOnVirtualRouter(vr, struct, new Completion(trigger) {
                                @Override
                                public void success() {
                                    destroyLoadBalancerOnHaRouter(vr.getUuid(), struct, new Completion(trigger) {
                                        @Override
                                        public void success() {
                                            proxy.detachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), asList(struct.getLb().getUuid()));
                                            trigger.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                        }
                                    });
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        } else {
                            throw new CloudRuntimeException(String.format("wrong virtual router roles%s. it doesn't have the role[%s]",
                                    roles, VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat()));
                        }
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void refresh(LoadBalancerStruct struct, Completion completion) {
        VirtualRouterVmInventory vr = findVirtualRouterVm(struct.getLb().getUuid());
        if (vr == null) {
            // the vr has been destroyed
            completion.success();
            return;
        }

        startVrIfNeededAndRefresh(vr, struct, completion);
    }

    /* this api is called from VirtualRouterSyncLbOnStartFlow which is specified to a individual router */
    public void syncOnStart(VirtualRouterVmInventory vr, boolean checkStatus, List<LoadBalancerStruct> structs, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("lb-sync-on-Start");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "lb-sync-certificate-on-start";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refreshCertificate(vr, checkStatus, structs, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        rollbackCertificate(vr, false, structs, new NoErrorCompletion(trigger) {
                            @Override
                            public void done() {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "lb-sync-listener-on-start";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<LbTO> tos = new ArrayList<LbTO>();
                        for (LoadBalancerStruct s : structs) {
                            tos.addAll(makeLbTOs(s, vr));
                        }

                        RefreshLbCmd cmd = new RefreshLbCmd();
                        cmd.lbs = tos;
                        cmd.enableHaproxyLog = rcf.getResourceConfigValue(VyosGlobalConfig.ENABLE_HAPROXY_LOG, vr.getUuid(), Boolean.class);

                        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setPath(REFRESH_LB_PATH);
                        msg.setVmInstanceUuid(vr.getUuid());
                        msg.setCheckStatus(checkStatus);
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());

                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    VirtualRouterAsyncHttpCallReply kr = reply.castReply();
                                    RefreshLbRsp rsp = kr.toResponse(RefreshLbRsp.class);
                                    if (rsp.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    }
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }

    protected List<VirtualRouterVmVO> getAllVirtualRouters(String lbUuid) {
        List<String> vrUuids = proxy.getVrUuidsByNetworkService(LoadBalancerVO.class.getSimpleName(), lbUuid);
        if (vrUuids == null || vrUuids.isEmpty()) {
            return new ArrayList<>();
        }

        return Q.New(VirtualRouterVmVO.class).in(VirtualRouterVmVO_.uuid, vrUuids).list();
    }

    public void destroyLoadBalancerOnVirtualRouter(VirtualRouterVmInventory vr, LoadBalancerStruct struct, Completion completion) {
        DeleteLbCmd cmd = new DeleteLbCmd();
        cmd.setLbs(makeLbTOs(struct, vr));
        if (cmd.lbs.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(DELETE_LB_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    DeleteLbRsp rsp = ((VirtualRouterAsyncHttpCallReply)reply).toResponse(DeleteLbRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private boolean isVirtualRouterHaPair(List<String> vrUuids) {
        boolean isVirtualRouterHaPair = false;
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            isVirtualRouterHaPair = ext.isVirtualRouterInSameHaPair(vrUuids);
        }

        return isVirtualRouterHaPair;
    }

    private void refreshCertificateOnHaRouter(String vrUuid, List<LoadBalancerStruct> structs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(REFRESH_CERTIFICATE_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(structs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private void rollbackCertificateOnHaRouter(String vrUuid, List<LoadBalancerStruct> structs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(DELETE_CERTIFICATE_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(structs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    protected void refreshLbToVirtualRouterHa(VirtualRouterVmInventory vrInv, LoadBalancerStruct struct, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(REFRESH_LB_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    protected void destroyLoadBalancerOnHaRouter(String vrUuid, LoadBalancerStruct struct, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(DESTROY_LB_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(struct));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private List<LoadBalancerStruct> getLoadBalancersByL3Networks(String l3Uuid, boolean detach) {
        List<LoadBalancerStruct> ret = new ArrayList<>();
        String sql = "select distinct l from LoadBalancerListenerVO l, LoadBalancerServerGroupVO grp, " +
                " LoadBalancerListenerServerGroupRefVO lgRef, VmNicVO nic, LoadBalancerServerGroupVmNicRefVO nicRef, " +
                " LoadBalancerVO lb where lb.type = :lbType and lb.uuid = l.loadBalancerUuid " +
                " and l.uuid = lgRef.listenerUuid and lgRef.serverGroupUuid = grp.uuid " +
                " and grp.uuid = nicRef.serverGroupUuid and nicRef.status in (:status) " +
                " and nicRef.vmNicUuid=nic.uuid and nic.l3NetworkUuid=(:l3Uuid)";

        List<LoadBalancerListenerVO> listenerVOS = SQL.New(sql, LoadBalancerListenerVO.class).param("l3Uuid", l3Uuid)
                .param("lbType", LoadBalancerType.Shared)
                .param("status", asList(LoadBalancerVmNicStatus.Active, LoadBalancerVmNicStatus.Pending)).list();
        if (listenerVOS == null || listenerVOS.isEmpty()){
            return ret;
        }

        HashMap<String, List<LoadBalancerListenerVO>> listenerMap = new HashMap<>();
        for (LoadBalancerListenerVO vo : listenerVOS) {
            listenerMap.computeIfAbsent(vo.getLoadBalancerUuid(), k-> new ArrayList<>()).add(vo);
        }

        for (Map.Entry<String, List<LoadBalancerListenerVO>> e : listenerMap.entrySet()) {
            LoadBalancerStruct struct = new LoadBalancerStruct();
            LoadBalancerVO lb = dbf.findByUuid(e.getKey(), LoadBalancerVO.class);
            struct.setLb(LoadBalancerInventory.valueOf(lb));
            struct.setVip(VipInventory.valueOf(dbf.findByUuid(lb.getVipUuid(), VipVO.class)));

            struct.setListenerServerGroupMap(new HashMap<>());
            List<String> serverGroupUuids = new ArrayList<>();
            for (LoadBalancerListenerVO listenerVO : e.getValue()) {
                List<String> uuids = listenerVO.getServerGroupRefs().stream().map(LoadBalancerListenerServerGroupRefVO::getServerGroupUuid).sorted().collect(Collectors.toList());
                if (!uuids.isEmpty()) {
                    List<LoadBalancerServerGroupVO> groupVOS = Q.New(LoadBalancerServerGroupVO.class)
                            .in(LoadBalancerServerGroupVO_.uuid, uuids).list();
                    struct.getListenerServerGroupMap().put(listenerVO.getUuid(), LoadBalancerServerGroupInventory.valueOf(groupVOS));
                    serverGroupUuids.addAll(uuids);
                }
            }
            HashMap<String, VmNicInventory> nicMap = new HashMap<>();
            if (!serverGroupUuids.isEmpty()) {
                sql = "select nic from LoadBalancerServerGroupVmNicRefVO ref, VmNicVO nic " +
                        "where nic.uuid=ref.vmNicUuid and ref.serverGroupUuid in (:serverGroupUuids) and ref.status in (:status)";

                List<VmNicVO> nicVOS = SQL.New(sql, VmNicVO.class).param("serverGroupUuids", serverGroupUuids)
                        .param("status", asList(LoadBalancerVmNicStatus.Active, LoadBalancerVmNicStatus.Pending)).list();
                if (nicVOS != null && !nicVOS.isEmpty()) {
                    for (VmNicVO nic : nicVOS) {
                        if (!detach) {
                            nicMap.put(nic.getUuid(), VmNicInventory.valueOf(nic));
                        } else {
                            /* when detach nic, vm nics of same l3 should not be included */
                            List<String> nicL3Uuids = nic.getUsedIps().stream().map(UsedIpVO::getL3NetworkUuid).collect(Collectors.toList());
                            if (!nicL3Uuids.contains(l3Uuid)) {
                                nicMap.put(nic.getUuid(), VmNicInventory.valueOf(nic));
                            }
                        }
                    }
                }
            }

            Map<String, List<String>> systemTags = new HashMap<>();
            for (LoadBalancerListenerVO l : listenerVOS) {
                List<String> list = Q.New(SystemTagVO.class)
                        .select(SystemTagVO_.tag)
                        .eq(SystemTagVO_.resourceUuid, l.getUuid())
                        .eq(SystemTagVO_.resourceType, LoadBalancerListenerVO.class.getSimpleName())
                        .listValues();
                systemTags.put(l.getUuid(), list);
            }

            struct.setListeners(LoadBalancerListenerInventory.valueOf(e.getValue()));
            struct.setVmNics(nicMap);
            struct.setTags(systemTags);
            ret.add(struct);
        }

        return ret;
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<LoadBalancerStruct> lbs = getLoadBalancersByL3Networks(nic.getL3NetworkUuid(), false);
        if (lbs == null || lbs.isEmpty()) {
            completion.success();
            return;
        }

        syncOnStart(vr, true, lbs, new Completion(completion) {
            @Override
            public void success() {
                List<String> lbUuids = lbs.stream().map(s -> s.getLb().getUuid()).collect(Collectors.toList());
                proxy.attachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), lbUuids);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
       /* ZSTAC-24726 for lb, it's not necessary to implement this interface
       * delete network/detach user vm nic, under these cases, the removeVmNics extend point will be triggered
       * that will remove the lb reference with nic first, and refresh lb to agent.
       * */
       completion.success();
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct refreshCertificate = new VirtualRouterHaCallbackStruct();
        refreshCertificate.type = REFRESH_CERTIFICATE_TASK;
        refreshCertificate.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need refresh certificate on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct[] s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct[].class);
                refreshCertificate(VirtualRouterVmInventory.valueOf(vrVO), false, Arrays.asList(s), completion);
            }
        };
        structs.add(refreshCertificate);

        VirtualRouterHaCallbackStruct deleteCertificate = new VirtualRouterHaCallbackStruct();
        deleteCertificate.type = DELETE_CERTIFICATE_TASK;
        deleteCertificate.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need delete certificate on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct[] s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct[].class);
                rollbackCertificate(VirtualRouterVmInventory.valueOf(vrVO), false, Arrays.asList(s), new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        completion.success();
                    }
                });
            }
        };
        structs.add(deleteCertificate);

        VirtualRouterHaCallbackStruct refreshLb = new VirtualRouterHaCallbackStruct();
        refreshLb.type = REFRESH_LB_TASK;
        refreshLb.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need refresh Lb on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct.class);
                refreshLbToVirtualRouter(VirtualRouterVmInventory.valueOf(vrVO), s, completion);
            }
        };
        structs.add(refreshLb);

        VirtualRouterHaCallbackStruct destroyLb = new VirtualRouterHaCallbackStruct();
        destroyLb.type = DESTROY_LB_TASK;
        destroyLb.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("VirtualRouter[uuid:%s] is deleted, no need d Lb on backend", vrUuid));
                    completion.success();
                    return;
                }

                LoadBalancerStruct s = JSONObjectUtil.toObject(task.getJsonData(), LoadBalancerStruct.class);
                destroyLoadBalancerOnVirtualRouter(VirtualRouterVmInventory.valueOf(vrVO), s, completion);
            }
        };
        structs.add(destroyLb);

        return structs;
    }

    protected List<String> getAttachableL3UuidsForVirtualRouter(VirtualRouterVmInventory vr, LoadBalancerInventory lb) {
        return vr.getGuestL3Networks();
    }

    @Override
    public List<VmNicVO> getAttachableVmNicsForServerGroup(LoadBalancerVO lbVO, LoadBalancerServerGroupVO groupVO) {
        List<String> attachedL3Uuids = new ArrayList<>();
        if (groupVO != null) {
            attachedL3Uuids = LoadBalancerServerGroupInventory.valueOf(groupVO).getAttachedL3Uuids();
        }

        List<String> l3NetworkUuids = new ArrayList<>();
        /* get vr of attached l3 */
        List<String> vrUuids = new ArrayList<>();
        VirtualRouterVmInventory vr = null;
        if (!attachedL3Uuids.isEmpty()) {
            vrUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                    .notNull(VmNicVO_.vmInstanceUuid)
                    .in(VmNicVO_.l3NetworkUuid, attachedL3Uuids)
                    .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).listValues();
        }
        if (vrUuids.isEmpty()) {
            vr = findVirtualRouterVm(lbVO.getUuid());
        } else {
            vr = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuids.get(0), VirtualRouterVmVO.class));
        }

        if (vr != null) {
            l3NetworkUuids = getAttachableL3UuidsForVirtualRouter(vr, LoadBalancerInventory.valueOf(lbVO));
        } else {
            VipVO vipVO = dbf.findByUuid(lbVO.getVipUuid(), VipVO.class);
            if (vipVO.isSystem()) {
                vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), vipVO.getUuid());
            } else {
                vrUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                        .eq(VmNicVO_.l3NetworkUuid, vipVO.getL3NetworkUuid()).notNull(VmNicVO_.metaData).listValues();
            }
            if (!vrUuids.isEmpty()) {
                List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid)
                        .in(VmNicVO_.vmInstanceUuid, vrUuids)
                        .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).listValues();
                l3NetworkUuids.addAll(l3Uuids);
            }
        }

        if (l3NetworkUuids.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "select l3.uuid from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                " where l3.uuid = ref.l3NetworkUuid and l3.uuid in (:l3NetworkUuids) and ref.networkServiceType = :type";
        l3NetworkUuids = SQL.New(sql, String.class).param("l3NetworkUuids", l3NetworkUuids)
                .param("type", LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING).list();
        if (l3NetworkUuids.isEmpty()) {
            return new ArrayList<>();
        }

        sql = "select nic from VmInstanceVO vm, VmNicVO nic " +
                " where vm.uuid=nic.vmInstanceUuid and vm.type in ('UserVM', 'baremetal2') and vm.state in (:vmStates)  " +
                " and nic.l3NetworkUuid in (:l3NetworkUuids) and nic.metaData is null and nic.ip is not null";
        List<VmNicVO> nicVOS = SQL.New(sql, VmNicVO.class)
                .param("l3NetworkUuids", l3NetworkUuids)
                .param("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped))
                .list();
        nicVOS = nicVOS.stream().filter(n -> !VmNicInventory.valueOf(n).isIpv6OnlyNic()).collect(Collectors.toList());

        if (groupVO != null) {
            List<String> attachedNicUuids = groupVO.getLoadBalancerServerGroupVmNicRefs().stream()
                    .map(LoadBalancerServerGroupVmNicRefVO::getVmNicUuid).collect(Collectors.toList());
            return nicVOS.stream().filter(n -> !attachedNicUuids.contains(n.getUuid())).collect(Collectors.toList());
        } else {
            return nicVOS;
        }
    }
}
