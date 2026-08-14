package org.zstack.vpc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlecode.ipv6.IPv6Address;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.ha.*;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.rest.RESTFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.*;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vpc.*;
import org.zstack.header.vpc.ha.*;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageSystemTags;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.IPsecConnectionVO;
import org.zstack.ipsec.vyos.VyosIPsecBackend;
import org.zstack.kvm.*;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.*;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.eip.EipConfigProxy;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.lb.LbConfigProxy;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingConfigProxy;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.network.service.virtualrouter.vyos.*;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.vpc.ha.VpcHaGroupOperator;
import org.zstack.vrouterRoute.VRouterRouteTableVO;
import org.zstack.vrouterRoute.VirtualRouterVRouterRouteTableRefVO;
import org.zstack.vrouterRoute.VirtualRouterVRouterRouteTableRefVO_;

import javax.persistence.TypedQuery;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.compute.vm.VmSystemTags.MACHINE_TYPE_TOKEN;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.network.service.virtualrouter.VirtualRouterConstant.*;
import static org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData.GUEST_NIC_MASK;
import static org.zstack.network.service.virtualrouter.vyos.VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by weiwang on 18/09/2017
 */
public class VpcManagerImpl extends AbstractService implements VpcManager, BeforeAcquireVirtualRouterVmExtensionPoint,
        KVMHostConnectExtensionPoint, VyosPostStartFlowExtensionPoint, VyosPostCreateFlowExtensionPoint,
        VyosPostRebootFlowExtensionPoint, VyosPostReconnectFlowExtensionPoint, FilterVmNicsForEipInVirtualRouterExtensionPoint,
        GetCandidateVmNicsForLoadBalancerExtensionPoint, GetPeerL3NetworksForLoadBalancerExtensionPoint,
        DnsServiceExtensionPoint, VirtualRouterHaGetCallbackExtensionPoint, VyosConnectExtensionPoint,
        VmFencerRuleExtensionPoint {
    private final static CLogger logger = Utils.getLogger(VpcManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ApplianceVmFacade apvmf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private VpcRouterDnsBackend dnsBackend;
    @Autowired
    private VyosIPsecBackend ipsecBackend;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected VipConfigProxy vipProxy;
    @Autowired
    protected EipConfigProxy eipProxy;
    @Autowired
    protected LbConfigProxy lbProxy;
    @Autowired
    protected PortForwardingConfigProxy pfProxy;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    protected ResourceConfigFacade rcf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    protected SnatConfigProxy snatProxy;

    public static final String ZSN_SET_DR_PATH = "/zsn/dr";
    public static final String ZSN_STATUS_PATH = "/zsn/status";
    public static final String ZSN_CONNECTION_PATH = "/zsn/connections";

    private final String APPLY_DISTRIBUTEROUTING_TASK = "applyDistributedRouting";
    private final String SET_NAT_TASK = "setSnat";
    private static Map<String, String> timeoutMap = new HashMap<>();

    private AddVmFencerRuleToHostMsg generateAddVmFencerRuleToHost(List<VmRuleAttachFencer> blockRules, List<VmRuleAttachFencer> allowRules, String hostUuid) {
        AddVmFencerRuleToHostMsg msg = new AddVmFencerRuleToHostMsg();
        msg.setBlockRules(blockRules);
        msg.setAllowRules(allowRules);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HaConstants.SERVICE_ID, msg.getHostUuid());
        return msg;
    }

    private RemoveVmFencerRuleFromHostMsg generateRemoveVmFencerRuleFromHost(List<VmRuleAttachFencer> blockRules, List<VmRuleAttachFencer> allowRules, String hostUuid) {
        RemoveVmFencerRuleFromHostMsg msg = new RemoveVmFencerRuleFromHostMsg();
        msg.setHostUuid(hostUuid);
        msg.setBlockRules(blockRules);
        msg.setAllowRules(allowRules);
        bus.makeTargetServiceIdByResourceUuid(msg, HaConstants.SERVICE_ID, msg.getHostUuid());
        return msg;
    }

    @Override
    public AddVmFencerRuleToHostMsg generateAddVmFencerRuleToHostMsg(List<String> vmUuids, String hostUuid) {
        List<String> vmUuidsOnHost = Q.New(VpcRouterVmVO.class)
                .in(VpcRouterVmVO_.uuid, vmUuids)
                .eq(VpcRouterVmVO_.hostUuid, hostUuid)
                .select(VpcRouterVmVO_.uuid)
                .listValues();
        if (CollectionUtils.isEmpty(vmUuidsOnHost)) {
            return null;
        }

        List<VmRuleAttachFencer> blockRules = new ArrayList<>();
        List<VmRuleAttachFencer> allowRules = new ArrayList<>();

        VmRuleAttachFencer blockFencer = new VmRuleAttachFencer();
        blockFencer.setFencerName(HaConstants.HOST_BUSINESS_NIC);
        blockFencer.setVmUuids(vmUuidsOnHost);
        blockRules.add(blockFencer);

        VmRuleAttachFencer allowFencer = new VmRuleAttachFencer();
        allowFencer.setFencerName(HaConstants.HOST_STORAGE_STATE);
        allowFencer.setVmUuids(vmUuidsOnHost);
        allowRules.add(allowFencer);

        return generateAddVmFencerRuleToHost(blockRules, allowRules, hostUuid);
    }

    @Override
    public RemoveVmFencerRuleFromHostMsg generateVmFencerRuleFromHostMsg(VmInstanceInventory inv, String hostUuid) {
        if (!Q.New(VpcRouterVmVO.class)
                .eq(VpcRouterVmVO_.uuid, inv.getUuid())
                .isExists()) {
            return null;
        }

        List<VmRuleAttachFencer> blockRules = new ArrayList<>();
        List<VmRuleAttachFencer> allowRules = new ArrayList<>();

        VmRuleAttachFencer blockFencer = new VmRuleAttachFencer();
        blockFencer.setFencerName(HaConstants.HOST_BUSINESS_NIC);
        List<String> vmUuids = new ArrayList<>();
        vmUuids.add(inv.getUuid());
        blockFencer.setVmUuids(vmUuids);
        blockRules.add(blockFencer);

        VmRuleAttachFencer allowFencer = new VmRuleAttachFencer();
        allowFencer.setFencerName(HaConstants.HOST_STORAGE_STATE);
        allowFencer.setVmUuids(vmUuids);
        allowRules.add(allowFencer);

        return generateRemoveVmFencerRuleFromHost(blockRules, allowRules, inv.getHostUuid());
    }

    @Override
    public void sendVmFencerRuleToHostMsg(NeedReplyMessage msg) {
        if (msg == null) {
            return;
        }
        bus.send(msg);
    }

    public static class GetStatusCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class GetStatusRsp extends VirtualRouterCommands.AgentResponse {
        public String rawStatus;
    }


    public static class SetDistributedRoutingCmd extends VirtualRouterCommands.AgentCommand {
        public Boolean enabled;
    }

    public static class SetDistributedRoutingRsp extends VirtualRouterCommands.AgentResponse {
        public String enabled;
    }

    public static class SetNetworkServiceSnatCmd extends VirtualRouterCommands.AgentCommand {
        private List<VirtualRouterCommands.SNATInfo> snats;

        public List<VirtualRouterCommands.SNATInfo> getSnats() {
            return snats;
        }

        public void setSnats(List<VirtualRouterCommands.SNATInfo> snats) {
            this.snats = snats;
        }
        public Boolean enabled;
    }

    public static class SetNetworkServiceRsp extends VirtualRouterCommands.AgentResponse {
        public String serviceStatus;

    }

    public static class GetConnectionsCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class GetConnectionsRsp extends VirtualRouterCommands.AgentResponse {
        public String rawConnections;
    }

    public static class ZsnStatus {
        private Boolean distributedRouting;
        private String tmout;

        public Boolean getDistributedRouting() {
            return distributedRouting;
        }

        public void setDistributedRouting(Boolean distributedRouting) {
            this.distributedRouting = distributedRouting;
        }

        public String getTmout() {
            return tmout;
        }

        public void setTmout(String tmout) {
            this.tmout = tmout;
        }
    }

    public static class VpcConnectionEntries {
        private Map<String, VpcConnectionEntry> entries;

        public Map<String, VpcConnectionEntry> getEntries() {
            return entries;
        }

        public void setEntries(Map<String, VpcConnectionEntry> entries) {
            this.entries = entries;
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVpcVRouterMsg) {
            handle((APICreateVpcVRouterMsg) msg);
        } else if (msg instanceof APIGetAttachableVpcL3NetworkMsg) {
            handle((APIGetAttachableVpcL3NetworkMsg) msg);
        } else if (msg instanceof APIGetVpcVRouterDistributedRoutingConnectionsMsg) {
            handle((APIGetVpcVRouterDistributedRoutingConnectionsMsg) msg);
        } else if (msg instanceof APIGetVpcVRouterDistributedRoutingEnabledMsg) {
            handle((APIGetVpcVRouterDistributedRoutingEnabledMsg) msg);
        } else if (msg instanceof APISetVpcVRouterDistributedRoutingEnabledMsg) {
            handle((APISetVpcVRouterDistributedRoutingEnabledMsg) msg);
        } else if (msg instanceof APIAddDnsToVpcRouterMsg) {
            handle((APIAddDnsToVpcRouterMsg) msg);
        } else if (msg instanceof APIRemoveDnsFromVpcRouterMsg) {
            handle((APIRemoveDnsFromVpcRouterMsg) msg);
        } else if (msg instanceof APISetVpcVRouterNetworkServiceStateMsg) {
            handle((APISetVpcVRouterNetworkServiceStateMsg) msg);
        } else if (msg instanceof APIUpdateVirtualRouterSoftwareVersionMsg) {
            handle((APIUpdateVirtualRouterSoftwareVersionMsg) msg);
        } else if (msg instanceof APIGetVpcVRouterNetworkServiceStateMsg) {
            handle((APIGetVpcVRouterNetworkServiceStateMsg) msg);
        } else if (msg instanceof APIGetVirtualRouterSoftwareVersionMsg) {
            handle((APIGetVirtualRouterSoftwareVersionMsg) msg);
        } else if (msg instanceof APIGetVpcAttachedEipMsg) {
            handle((APIGetVpcAttachedEipMsg) msg);
        } else if (msg instanceof APIGetVpcAttachedLoadBalancerMsg) {
            handle((APIGetVpcAttachedLoadBalancerMsg) msg);
        } else if (msg instanceof APIGetVpcAttachedPortForwardingRulesMsg) {
            handle((APIGetVpcAttachedPortForwardingRulesMsg) msg);
        } else if (msg instanceof APIGetVpcAttachedVipMsg) {
            handle((APIGetVpcAttachedVipMsg) msg);
        } else if (msg instanceof APIGetVpcAttachedIpsecMsg) {
            handle((APIGetVpcAttachedIpsecMsg) msg);
        } else if (msg instanceof APIGetRouteTableVpcVRouterCandidateMsg) {
            handle((APIGetRouteTableVpcVRouterCandidateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetRouteTableVpcVRouterCandidateMsg msg) {
        APIGetRouteTableVpcVRouterCandidateReply reply = new APIGetRouteTableVpcVRouterCandidateReply();

        List<String> ret = new ArrayList<>();

        /* get all vpc router and vpc ha master router */
        List<String> noHaUuids = Q.New(VpcRouterVmVO.class).select(VpcRouterVmVO_.uuid)
                .eq(VpcRouterVmVO_.haStatus, ApplianceVmHaStatus.NoHa).listValues();
        if (msg.getTableUuid() != null) {
            List<String> attachedUuids = Q.New(VirtualRouterVRouterRouteTableRefVO.class)
                    .eq(VirtualRouterVRouterRouteTableRefVO_.routeTableUuid, msg.getTableUuid())
                    .select(VirtualRouterVRouterRouteTableRefVO_.virtualRouterVmUuid).listValues();
            if (!attachedUuids.isEmpty()) {
                noHaUuids.removeAll(attachedUuids);
            }
        }
        if (!noHaUuids.isEmpty()) {
            ret.addAll(noHaUuids);
        }

        List<String> haGroupUuids = Q.New(VpcHaGroupVO.class).select(VpcHaGroupVO_.uuid).listValues();
        if (msg.getTableUuid() != null) {
            List<String> attachedUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                    .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VRouterRouteTableVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, msg.getTableUuid()).listValues();
            if (!attachedUuids.isEmpty()) {
                haGroupUuids.removeAll(attachedUuids);
            }
        }
        List<String> masterUuids = haGroupUuids.stream()
                .map(VpcHaGroupOperator::getMasterUuidByVpcHaRouterUuid).filter(Objects::nonNull).collect(Collectors.toList());
        if (!masterUuids.isEmpty()) {
            ret.addAll(masterUuids);
        }

        if (ret.isEmpty()) {
            reply.setInventories(Collections.emptyList());
            bus.reply(msg, reply);
            return;
        }

        List<VpcRouterVmVO> vpcInventories = Q.New(VpcRouterVmVO.class).in(VpcRouterVmVO_.uuid, ret).list();

        reply.setInventories(VpcRouterVmInventory.valueOf3(vpcInventories));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVpcAttachedEipMsg msg) {
        APIGetVpcAttachedEipReply reply = new APIGetVpcAttachedEipReply();
        List<String> eipUuids = eipProxy.getServiceUuidsByRouterUuid(msg.getUuid(), EipVO.class.getSimpleName());
        if (eipUuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<EipVO> eips = Q.New(EipVO.class).in(EipVO_.uuid, eipUuids).limit(msg.getLimit()).start(msg.getStart()).list();
        reply.setInventories(EipInventory.valueOf(eips));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVpcAttachedLoadBalancerMsg msg) {
        APIGetVpcAttachedLoadBalancerReply reply = new APIGetVpcAttachedLoadBalancerReply();
        List<String> lbUuids = lbProxy.getServiceUuidsByRouterUuid(msg.getUuid(), LoadBalancerVO.class.getSimpleName());
        if (lbUuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<LoadBalancerVO> lbs = Q.New(LoadBalancerVO.class).in(LoadBalancerVO_.uuid, lbUuids)
                .limit(msg.getLimit()).start(msg.getStart()).list();
        reply.setInventories(LoadBalancerInventory.valueOf(lbs));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVpcAttachedPortForwardingRulesMsg msg) {
        APIGetVpcAttachedPortForwardingRulesReply reply = new APIGetVpcAttachedPortForwardingRulesReply();
        List<String> pfUuids = pfProxy.getServiceUuidsByRouterUuid(msg.getUuid(), PortForwardingRuleVO.class.getSimpleName());
        if (pfUuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<PortForwardingRuleVO> pfs = Q.New(PortForwardingRuleVO.class).in(PortForwardingRuleVO_.uuid, pfUuids)
                .limit(msg.getLimit()).start(msg.getStart()).list();
        reply.setInventories(PortForwardingRuleInventory.valueOf(pfs));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVpcAttachedVipMsg msg) {
        APIGetVpcAttachedVipReply reply = new APIGetVpcAttachedVipReply();
        List<String> vipUuids = vipProxy.getServiceUuidsByRouterUuid(msg.getUuid(), VipVO.class.getSimpleName());
        if (vipUuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<VipVO> vips = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids)
                .limit(msg.getLimit()).start(msg.getStart()).list();
        reply.setInventories(VipInventory.valueOf(vips));
        bus.reply(msg, reply);
    }

    private void handle(APIGetVpcAttachedIpsecMsg msg) {
        APIGetVpcAttachedIpsecReply reply = new APIGetVpcAttachedIpsecReply();
        VirtualRouterVmVO vr = dbf.findByUuid(msg.getUuid(), VirtualRouterVmVO.class);
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vr);
        List<String> l3Uuids = vrInv.getGuestL3Networks();
        if (l3Uuids.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<IPsecConnectionVO> ipsecs = SQL.New("select distinct ipsec from IPsecConnectionVO ipsec, IPsecL3NetworkRefVO ref " +
                "where ipsec.uuid=ref.connectionUuid and ref.l3NetworkUuid in (:l3Uuids)", IPsecConnectionVO.class)
                .param("l3Uuids", l3Uuids).limit(msg.getLimit()).offset(msg.getStart())
                .list();
        reply.setInventories(IPsecConnectionInventory.valueOf(ipsecs));
        bus.reply(msg, reply);
    }
    void setVpcRouterDistributedRouting(String vrUuid, String ret) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVO.isHaEnabled()) {
            if (VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED.hasTag(vrUuid)) {
                VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED.delete(vrUuid);
            }
            SystemTagCreator creator = VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED.newSystemTagCreator(vrUuid);
            creator.ignoreIfExisting = false;
            creator.inherent = false;
            creator.setTagByTokens(
                    map(
                            e(VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED_TOKEN, ret)
                    )
            );

            creator.create();
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.attachNetworkServiceToHaRouter(VpcConstants.VR_DR_STATE, asList(ret), vrUuid, true);
            }
        }
    }

    @Override
    public String getVpcRouterDistributedRouting(String vrUuid) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVO.isHaEnabled()) {
            return VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED.getTokenByResourceUuid(
                    vrUuid, VirtualRouterVmVO.class, VpcSystemTags.VPC_DISTRIBUTED_ROUTING_ENABLED_TOKEN);
        } else {
            List<VirtualRouterHaGroupExtensionPoint> exp = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
            if (exp == null || exp.isEmpty()) {
                return null;
            }

            List<String> res = exp.get(0).getNetworkServicesFromHaVrUuid(VpcConstants.VR_DR_STATE,  vrUuid);
            if (res == null || res.isEmpty()) {
                return null;
            } else {
                return res.get(0);
            }
        }
    }

    public void handle(APIGetVpcVRouterDistributedRoutingConnectionsMsg msg) {
        GetConnectionsCmd cmd = new GetConnectionsCmd();
        APIGetVpcVRouterDistributedRoutingConnectionsReply reply = new APIGetVpcVRouterDistributedRoutingConnectionsReply();

        VirtualRouterAsyncHttpCallMsg vmsg = new VirtualRouterAsyncHttpCallMsg();
        vmsg.setCommand(cmd);
        vmsg.setVmInstanceUuid(msg.getUuid());
        vmsg.setCheckStatus(true);
        vmsg.setPath(ZSN_CONNECTION_PATH);
        bus.makeTargetServiceIdByResourceUuid(vmsg, VmInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(vmsg, new CloudBusCallBack(msg) {
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                    bus.reply(msg, reply);
                    return;
                }

                VirtualRouterAsyncHttpCallReply ar = r.castReply();
                GetConnectionsRsp rsp = ar.toResponse(GetConnectionsRsp.class);
                if (!rsp.isSuccess()) {
                    reply.setError(operr("can not get connections of distributed routing to virtual router %s", msg.getUuid()));
                    bus.reply(msg, reply);
                    return;
                }

                Type VpcConnectionEntries = new TypeToken<Map<String, VpcConnectionEntry>>() {}.getType();
                Map<String, Object> entries = new Gson().fromJson(rsp.rawConnections, VpcConnectionEntries);

                if (entries.values().isEmpty()) {
                    reply.setInventories(entries);
                } else {
                    reply.setInventories(VpcConnectionTO.valueOf1(entries));
                }

                bus.reply(msg, reply);
            }
        });
    }

    private void applyVpcVRouterDistributedRoutingToRouter(VirtualRouterVmInventory vrinv, SetDistributedRoutingCmd cmd,
                                                           ReturnValueCompletion<String> completion) {
        VirtualRouterAsyncHttpCallMsg vmsg = new VirtualRouterAsyncHttpCallMsg();
        vmsg.setCommand(cmd);
        vmsg.setVmInstanceUuid(vrinv.getUuid());
        vmsg.setPath(ZSN_SET_DR_PATH);
        vmsg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(vmsg, VmInstanceConstant.SERVICE_ID, vrinv.getUuid());
        bus.send(vmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                SetDistributedRoutingRsp rsp = ar.toResponse(SetDistributedRoutingRsp.class);
                if (rsp == null || rsp.enabled == null || rsp.enabled.equals("")) {
                    completion.fail(operr("can not set state of distributed routing to virtual router %s", vrinv.getUuid()));
                    return;
                }

                completion.success(rsp.enabled);
            }
        });
    }

    private void submitApplyVpcVRouterDistributedRoutingToHaRouter(VirtualRouterVmInventory vrInv, SetDistributedRoutingCmd cmd, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(APPLY_DISTRIBUTEROUTING_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(cmd));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    public void handle(APISetVpcVRouterDistributedRoutingEnabledMsg msg) {
        APISetVpcVRouterDistributedRoutingEnabledEvent event = new APISetVpcVRouterDistributedRoutingEnabledEvent(msg.getId());
        SetDistributedRoutingCmd cmd = new SetDistributedRoutingCmd();
        cmd.enabled = VpcStateEvent.valueOf(msg.getStateEvent()).equals(VpcStateEvent.enable);

        final VirtualRouterVmVO vo = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, msg.getUuid()).find();
        final VirtualRouterVmInventory vrinv = VirtualRouterVmInventory.valueOf(vo);
        applyVpcVRouterDistributedRoutingToRouter(vrinv, cmd, new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String ret) {
                if (ret.equals("true")) {
                    setVpcRouterDistributedRouting(vrinv.getUuid(), "enabled");
                } else {
                    setVpcRouterDistributedRouting(vrinv.getUuid(), "disabled");
                }

                submitApplyVpcVRouterDistributedRoutingToHaRouter(vrinv, cmd, new Completion(msg) {
                    @Override
                    public void success() {
                        event.setEnabled(new VpcDistributedRoutingGetter().getState(msg.getUuid()));
                        bus.publish(event);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    public void handle(APIGetVpcVRouterDistributedRoutingEnabledMsg msg) {
        APIGetVpcVRouterDistributedRoutingEnabledReply reply = new APIGetVpcVRouterDistributedRoutingEnabledReply();
        GetStatusCmd cmd = new GetStatusCmd();

        VirtualRouterAsyncHttpCallMsg vmsg = new VirtualRouterAsyncHttpCallMsg();
        vmsg.setCommand(cmd);
        vmsg.setCheckStatus(true);
        vmsg.setVmInstanceUuid(msg.getUuid());
        vmsg.setPath(ZSN_STATUS_PATH);
        bus.makeTargetServiceIdByResourceUuid(vmsg, VmInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(vmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply mreply) {
                if (!mreply.isSuccess()) {
                    reply.setError(mreply.getError());
                    bus.reply(msg, reply);
                    return;
                }

                VirtualRouterAsyncHttpCallReply ar = mreply.castReply();
                GetStatusRsp rsp = ar.toResponse(GetStatusRsp.class);
                if (rsp == null || rsp.rawStatus == null || rsp.rawStatus.equals("")) {
                    reply.setEnabled(false);
                    bus.reply(msg, reply);
                    return;
                }

                Gson gson = new Gson();
                ZsnStatus s = gson.fromJson(rsp.rawStatus, ZsnStatus.class);

                reply.setEnabled(s.getDistributedRouting());
                bus.reply(msg, reply);
            }
        });
    }

    public void handle(APIGetVirtualRouterSoftwareVersionMsg msg) {
        APIGetVirtualRouterSoftwareVersionReply reply = new APIGetVirtualRouterSoftwareVersionReply();
        List<VirtualRouterSoftwareVersionVO> virtualRouterSoftwareVersionList = new ArrayList<>();

        //Get NetworkService IPsec Version
        if(IPSEC_NETWORK_SERVICE_TYPE.equals(msg.getSoftwareName())) {
            if (msg.getUuid() != null && !msg.getUuid().isEmpty()) {
                virtualRouterSoftwareVersionList.addAll(Q.New(VirtualRouterSoftwareVersionVO.class)
                        .eq(VirtualRouterSoftwareVersionVO_.uuid, msg.getUuid())
                        .eq(VirtualRouterSoftwareVersionVO_.softwareName, msg.getSoftwareName())
                        .list());
            } else if (msg.getNeedUpdate()){
                virtualRouterSoftwareVersionList.addAll(Q.New(VirtualRouterSoftwareVersionVO.class)
                        .eq(VirtualRouterSoftwareVersionVO_.softwareName, msg.getSoftwareName())
                        .notEq(VirtualRouterSoftwareVersionVO_.currentVersion, IPSEC_NETWORK_SERVICE_LATEST_VERSION)
                        .list());
            } else {
                virtualRouterSoftwareVersionList.addAll(Q.New(VirtualRouterSoftwareVersionVO.class)
                        .eq(VirtualRouterSoftwareVersionVO_.softwareName, msg.getSoftwareName())
                        .list());
            }
            reply.setInventories(VirtualRouterSoftwareVersionInventory.valueOf(virtualRouterSoftwareVersionList));
        }

        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    public void handle(APIGetVpcVRouterNetworkServiceStateMsg msg) {
        APIGetVpcVRouterNetworkServiceStateReply reply = new APIGetVpcVRouterNetworkServiceStateReply();

        if (!SNAT_NETWORK_SERVICE_TYPE.equals(msg.getNetworkService())) {
            reply.setError(operr("not support to get the service %s state to virtual router %s", msg.getNetworkService(), msg.getUuid()));
            reply.setSuccess(false);
            bus.reply(msg, reply);
            return;
        }

        if (msg.getL3NetworkUuid() == null) {
            String defaultL3Uuid = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, msg.getUuid())
                    .select(VirtualRouterVmVO_.defaultRouteL3NetworkUuid).findValue();
            msg.setL3NetworkUuid(defaultL3Uuid);
        }

        if (getSnatStateFromVpcRouter(msg.getUuid(), msg.getL3NetworkUuid())) {
            reply.setState(VpcStateEvent.enable.toString());
        } else {
            reply.setState(VpcStateEvent.disable.toString());
        }
        reply.setSuccess(true);
        bus.reply(msg, reply);
    }

    void setVpcRouterSnatServicesRef(String l3Uuid, VmNicInventory publicNic, String state, String vpcUuid) {
        if (VpcStateEvent.valueOf(state).equals(VpcStateEvent.disable)) {
            VipVO vip = Q.New(VipVO.class).eq(VipVO_.l3NetworkUuid, l3Uuid)
                    .eq(VipVO_.ip, publicNic.getIp())
                    .eq(VipVO_.system, true).find();
            VipNetworkServicesRefVO vipRef = Q.New(VipNetworkServicesRefVO.class)
                    .eq(VipNetworkServicesRefVO_.uuid, vpcUuid)
                    .eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid())
                    .eq(VipNetworkServicesRefVO_.serviceType, VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE)
                    .find();
            if (vipRef != null) {
                dbf.remove(vipRef);
            }
        } else {
            VipVO vip = Q.New(VipVO.class).eq(VipVO_.l3NetworkUuid, l3Uuid)
                    .eq(VipVO_.ip, publicNic.getIp())
                    .eq(VipVO_.system, true).find();
            if(!Q.New(VipNetworkServicesRefVO.class)
                    .eq(VipNetworkServicesRefVO_.uuid, vpcUuid)
                    .eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid())
                    .eq(VipNetworkServicesRefVO_.serviceType, VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE).isExists()) {
                VipNetworkServicesRefVO vipRef = new VipNetworkServicesRefVO();
                vipRef.setUuid(vpcUuid);
                vipRef.setServiceType(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE);
                vipRef.setVipUuid(vip.getUuid());
                dbf.persistAndRefresh(vipRef);
            }
        }
    }

    void disableVpcRouterSnat(String vrUuid, String l3NetworkUuid) {
        snatProxy.detachNetworkService(vrUuid, NetworkServiceType.SNAT.toString(), asList(l3NetworkUuid));
    }

    void enableVpcRouterSnat(String vrUuid, String l3NetworkUuid) {
        snatProxy.attachNetworkService(vrUuid, NetworkServiceType.SNAT.toString(), asList(l3NetworkUuid));
    }

    public boolean getSnatStateFromVpcRouter(String vrUuid, String l3NetworkUuid) {
        List<String> l3NetworkUuids = snatProxy.getServiceUuidsByRouterUuid(vrUuid, NetworkServiceType.SNAT.toString());

        return l3NetworkUuids.contains(l3NetworkUuid);
    }

    private void submitSetSnatStateToHaRouter(VirtualRouterVmInventory vrInv, VmNicInventory publicNic, boolean state, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(SET_NAT_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());

        SetSnatStateTaskData d = new SetSnatStateTaskData();
        d.setPublicNic(publicNic);
        d.setState(state);
        task.setJsonData(JSONObjectUtil.toJsonString(d));

        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private void setSnatStateToVirtualRouter(VirtualRouterVmInventory vrinv, VmNicInventory publicNic, boolean state, ReturnValueCompletion<String> completion) {
        SetNetworkServiceSnatCmd cmd = new SetNetworkServiceSnatCmd();
        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<VirtualRouterCommands.SNATInfo>();
        String publicIp4 = null;
        for (UsedIpInventory ip : publicNic.getUsedIps()) {
            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                publicIp4 = ip.getIp();
            }
        }
        if (publicIp4 == null) {
            completion.success(Boolean.toString(state));
            return;
        }

        for (VmNicInventory nic : vrinv.getVmNics()) {
            if (vrMgr.isL3NetworkNeedingNetworkServiceByVirtualRouter(nic.getL3NetworkUuid(), NetworkServiceType.SNAT.toString())) {
                for (UsedIpInventory ip: nic.getUsedIps()) {
                    if (ip.getIpVersion() != IPv6Constants.IPv4) {
                        continue;
                    }
                    VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
                    info.setPrivateNicIp(ip.getIp());
                    info.setPrivateNicMac(nic.getMac());
                    info.setPublicIp(publicIp4);
                    info.setPublicNicMac(publicNic.getMac());
                    info.setSnatNetmask(ip.getNetmask());
                    info.setState(state);
                    info.setPrivateGatewayIp(nic.getGateway());
                    snatInfo.add(info);
                }
            }
        }
        if (snatInfo.isEmpty()) {
            completion.success(Boolean.toString(state));
            return;
        }

        cmd.setSnats(snatInfo);
        cmd.enabled = state;

        VirtualRouterAsyncHttpCallMsg vmsg = new VirtualRouterAsyncHttpCallMsg();
        vmsg.setCheckStatus(true);
        vmsg.setCommand(cmd);
        vmsg.setVmInstanceUuid(vrinv.getUuid());
        vmsg.setPath(VpcConstants.VR_SET_VPC_NETWORK_SERVICE_SNAT_STATE_PATH);
        bus.makeTargetServiceIdByResourceUuid(vmsg, VmInstanceConstant.SERVICE_ID, vrinv.getUuid());
        bus.send(vmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                SetNetworkServiceRsp rsp = ar.toResponse(SetNetworkServiceRsp.class);
                if (rsp == null || rsp.serviceStatus == null || rsp.serviceStatus.equals("")) {
                    completion.fail(operr("can not get state of distributed routing to virtual router %s", vrinv.getUuid()));
                    return;
                }
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                completion.success(rsp.serviceStatus);
            }
        });
    }

    public void handle(APIUpdateVirtualRouterSoftwareVersionMsg msg) {
        APIUpdateVirtualRouterSoftwareVersionEvent evt = new APIUpdateVirtualRouterSoftwareVersionEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();

        chain.setName(String.format("update-networkService-%s-version-to-%s-at-vpcrouter-%s", msg.getSoftwareName(), msg.getTargetVersion(), msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if(IPSEC_NETWORK_SERVICE_TYPE.equals(msg.getSoftwareName())) {
                            ipsecBackend.updateIpsecVersion(msg.getUuid(), msg.getTargetVersion(), new Completion(trigger) {
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
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    public void handle(APISetVpcVRouterNetworkServiceStateMsg msg) {
        /*
         * 1. check if the services have been disabled, if yes, return directly
         * 2. send the cmd to vpc agent to remove the services
         * 3. update the systag in DB
         */
        APISetVpcVRouterNetworkServiceStateEvent event = new APISetVpcVRouterNetworkServiceStateEvent(msg.getId());

        final VirtualRouterVmVO vo = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, msg.getUuid()).find();
        final VirtualRouterVmInventory vrinv = VirtualRouterVmInventory.valueOf(vo);
        String haGroupUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrinv.getUuid());
        final String vpcUuid = (haGroupUuid == null) ? vrinv.getUuid() : haGroupUuid;

        if (msg.getL3NetworkUuid() == null) {
            msg.setL3NetworkUuid(vrinv.getDefaultRouteL3NetworkUuid());
        }

        if (!SNAT_NETWORK_SERVICE_TYPE.equals(msg.getNetworkService())) {
            event.setError(operr("not support to update the service %s state to virtual router %s", msg.getNetworkService(), msg.getUuid()));
            event.setSuccess(false);
            bus.publish(event);
            return;
        }

        boolean snatState = getSnatStateFromVpcRouter(msg.getUuid(), msg.getL3NetworkUuid());
         if ((VpcStateEvent.valueOf(msg.getState()).equals(VpcStateEvent.disable) && !snatState) ||
                 (VpcStateEvent.valueOf(msg.getState()).equals(VpcStateEvent.enable) && snatState) ) {
            event.setState(msg.getState());
            event.setSuccess(true);
            bus.publish(event);
            return;
        }
        /* for management and public is same case, snat ip should use the ip of public vip, not the nic ip
         * getSnatPubicInventory will replace nic ip with the vip */
        VmNicInventory publicNic = vrMgr.getSnatPubicInventory(vrinv, msg.getL3NetworkUuid());;

        List<String> nwServed = vrinv.getAllL3Networks();
        nwServed = vrMgr.selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.SNAT);
        if (nwServed.isEmpty()) {
            if (VpcStateEvent.valueOf(msg.getState()).equals(VpcStateEvent.disable)) {
                disableVpcRouterSnat(msg.getUuid(), msg.getL3NetworkUuid());
            } else {
                enableVpcRouterSnat(msg.getUuid(), msg.getL3NetworkUuid());
            }
            setVpcRouterSnatServicesRef(msg.getL3NetworkUuid(), publicNic, msg.getState(), vpcUuid);
            event.setState(msg.getState());
            event.setSuccess(true);
            bus.publish(event);
            return;
        }

        boolean state = VpcStateEvent.valueOf(msg.getState()) == VpcStateEvent.enable;
        final VmNicInventory finalNic = publicNic;
        setSnatStateToVirtualRouter(vrinv, finalNic, state, new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String ret) {
                if (VpcStateEvent.valueOf(msg.getState()).equals(VpcStateEvent.disable)) {
                    disableVpcRouterSnat(msg.getUuid(), msg.getL3NetworkUuid());
                } else {
                    enableVpcRouterSnat(msg.getUuid(), msg.getL3NetworkUuid());
                }

                submitSetSnatStateToHaRouter(vrinv, finalNic, state, new Completion(msg) {
                    @Override
                    public void success() {
                        setVpcRouterSnatServicesRef(msg.getL3NetworkUuid(), finalNic, msg.getState(), vpcUuid);
                        event.setState(ret);
                        bus.publish(event);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                    }
                });

            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetAttachableVpcL3NetworkMsg msg) {
        APIGetAttachableVpcL3NetworkReply reply = new APIGetAttachableVpcL3NetworkReply();
        List<L3NetworkVO> result = new ArrayList<>();
        List<L3NetworkVO> vpcL3s;

        List<String> l3Uuids = acntMgr.getResourceUuidsCanAccessByAccount(msg.getSession().getAccountUuid(), L3NetworkVO.class);

        if (l3Uuids == null) {
            // this means the account is SystemAdmin
            vpcL3s = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.category, L3NetworkCategory.Private)
                    .eq(L3NetworkVO_.type, VpcConstants.VPC_L3_NETWORK_TYPE)
                    .list();
        } else if (l3Uuids.isEmpty()) {
            // this means there are no resource accessible to this account
            reply.setInventories(L3NetworkInventory.valueOf(result));
            bus.reply(msg, reply);
            return;
        } else {
            // get vpc networks
            vpcL3s = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.category, L3NetworkCategory.Private)
                    .eq(L3NetworkVO_.type, VpcConstants.VPC_L3_NETWORK_TYPE)
                    .in(L3NetworkVO_.uuid, l3Uuids)
                    .list();
        }
        if (vpcL3s == null || vpcL3s.isEmpty()) {
            reply.setInventories(L3NetworkInventory.valueOf(result));
            bus.reply(msg, reply);
            return;
        }

        VirtualRouterVmVO routerVmVO = Q.New(VirtualRouterVmVO.class)
                .eq(VirtualRouterVmVO_.uuid, msg.getUuid())
                .find();
        List<UsedIpVO> usedIps = routerVmVO.getUsedIps();
        Set<String> existsGateways = usedIps.stream().filter(ip -> ip.getIpVersion() == IPv6Constants.IPv4)
                .map(UsedIpVO::getGateway).collect(Collectors.toSet());
        Set<String> existsGateway6s = usedIps.stream().filter(ip -> ip.getIpVersion() == IPv6Constants.IPv6)
                .map(UsedIpVO::getGateway).collect(Collectors.toSet());
        Set<NormalIpRangeVO> existsIpRanges = new HashSet<>();
        Set<NormalIpRangeVO> existsIp6Ranges = new HashSet<>();
        for (UsedIpVO usedIpVO : usedIps) {
            NormalIpRangeVO ipr = Q.New(NormalIpRangeVO.class)
                    .eq(NormalIpRangeVO_.uuid, usedIpVO.getIpRangeUuid())
                    .find();
            if (ipr.getIpVersion() == IPv6Constants.IPv4) {
                existsIpRanges.add(ipr);
            } else {
                existsIp6Ranges.add(ipr);
            }
        }

        for (L3NetworkVO vo : vpcL3s) {
            List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(vo, IPv6Constants.IPv4);
            List<IpRangeInventory> ipr6s = IpRangeHelper.getNormalIpRanges(vo, IPv6Constants.IPv6);

            /* vpc network already attached or no ip range in the vpc l3 */
            if (Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, vo.getUuid())
                    .eq(VmNicVO_.metaData, GUEST_NIC_MASK)
                    .isExists() || (iprs.isEmpty() && ipr6s.isEmpty())) {
                continue;
            }

            /* l2 network doesn't attached to the cluster */
            L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class)
                    .eq(L2NetworkVO_.uuid, vo.getL2NetworkUuid())
                    .find();
            if (l2NetworkVO.getAttachedClusterRefs() == null ||
                    l2NetworkVO.getAttachedClusterRefs().isEmpty()) {
                continue;
            }

            if (l2NetworkVO.getAttachedClusterRefs().stream().noneMatch(r -> r.getClusterUuid().equals(
                    routerVmVO.getClusterUuid()))) {
                continue;
            }

            /* new network has same gateway with old nic */
            if (!existsGateways.isEmpty() && !iprs.isEmpty() && existsGateways.contains(iprs.get(0).getGateway())) {
                continue;
            }

            if (!existsGateway6s.isEmpty() && !ipr6s.isEmpty() && existsGateway6s.contains(ipr6s.get(0).getGateway())) {
                continue;
            }

            /* new network has same overlapped ip range with old nic */
            if (!iprs.isEmpty() && existsIpRanges.stream()
                    .anyMatch(r -> NetworkUtils.isCidrOverlap(r.getNetworkCidr(), iprs.get(0).getNetworkCidr()))) {
                continue;
            }

            if (!ipr6s.isEmpty() && existsIp6Ranges.stream()
                    .anyMatch(r -> IPv6NetworkUtils.isIpv6RangeOverlap(r.getStartIp(), r.getEndIp(),
                            ipr6s.get(0).getStartIp(), ipr6s.get(0).getEndIp()))) {
                continue;
            }

            result.add(vo);
        }

        reply.setInventories(L3NetworkInventory.valueOf(result));
        bus.reply(msg, reply);
    }

    private void handle(APICreateVpcVRouterMsg msg) {
        APICreateVpcVRouterEvent event = new APICreateVpcVRouterEvent(msg.getId());
        createVirtualRouter(msg, new ReturnValueCompletion<VirtualRouterVmInventory>(msg) {
            @Override
            public void success(VirtualRouterVmInventory returnValue) {
                event.setInventory(returnValue);
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                event.setSuccess(false);
                bus.publish(event);
            }
        });
    }

    @Override
    public void createVirtualRouter(APICreateVpcVRouterMsg msg, ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        final VirtualRouterOfferingInventory offering = VirtualRouterOfferingInventory.valueOf(
                (VirtualRouterOfferingVO) Q.New(VirtualRouterOfferingVO.class)
                .eq(VirtualRouterOfferingVO_.uuid, msg.getVirtualRouterOfferingUuid()).find());
        final int agentPort = VirtualRouterGlobalProperty.AGENT_PORT;
        final String accountUuid = msg.getSession().getAccountUuid();

        class newVirtualRouterJob {
            private void failAndReply(ErrorCode err) {
                completion.fail(err);
            }

            private void createApplianceVmHaSpecRollBack(Iterator<ApplianceVmHaExtensionPoint> it, ApplianceVmSpec aspec, String offeringUuid, String haUuid, NoErrorCompletion completion) {
                if (!it.hasNext()) {
                    completion.done();
                    return;
                }

                ApplianceVmHaExtensionPoint next = it.next();
                next.createApplianceVmHaSpecRollBack(aspec, offeringUuid, haUuid, new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        createApplianceVmHaSpecRollBack(it, aspec, offeringUuid, haUuid, completion);
                    }
                });
            }

            private void openFirewall(ApplianceVmSpec aspec, String l3NetworkUuid, int port, ApplianceVmFirewallProtocol protocol) {
                ApplianceVmFirewallRuleInventory r = new ApplianceVmFirewallRuleInventory();
                r.setL3NetworkUuid(l3NetworkUuid);
                r.setStartPort(port);
                r.setEndPort(port);
                r.setProtocol(protocol.toString());
                aspec.getFirewallRules().add(r);
            }

            private void openAdditionalPorts(ApplianceVmSpec aspec, String mgmtNwUuid) {
                final List<String> tcpPorts = VirtualRouterGlobalProperty.TCP_PORTS_ON_MGMT_NIC;
                if (!tcpPorts.isEmpty()) {
                    List<Integer> ports = transformAndRemoveNull(tcpPorts, Integer::valueOf);
                    for (int p : ports) {
                        openFirewall(aspec, mgmtNwUuid, p, ApplianceVmFirewallProtocol.tcp);
                    }
                }

                final List<String> udpPorts = VirtualRouterGlobalProperty.UDP_PORTS_ON_MGMT_NIC;
                if (!udpPorts.isEmpty()) {
                    List<Integer> ports = udpPorts.stream()
                            .map(Integer::valueOf)
                            .collect(Collectors.toList());
                    for (int p : ports) {
                        openFirewall(aspec, mgmtNwUuid, p, ApplianceVmFirewallProtocol.udp);
                    }
                }
            }

            private void create() {

                if (offering.getPublicNetworkUuid() == null) {
                    ErrorCode err = err(VirtualRouterErrors.NO_PUBLIC_NETWORK_IN_OFFERING, "virtual router offering[uuid:%s, name:%s] doesn't have a public network", offering.getUuid(), offering.getName());
                    logger.warn(err.getDetails());
                    failAndReply(err);
                    return;
                }

                ImageVO imgvo = dbf.findByUuid(offering.getImageUuid(), ImageVO.class);

                final ApplianceVmSpec aspec = new ApplianceVmSpec();
                aspec.setUuid(msg.getResourceUuid());
                aspec.setSyncCreate(false);
                aspec.setTemplate(ImageInventory.valueOf(imgvo));
                aspec.setApplianceVmType(ApplianceVmType.valueOf(VpcConstants.VPC_VROUTER_VM_TYPE));
                aspec.setInstanceOffering(offering);
                aspec.setName(msg.getName());
                aspec.setDescription(msg.getDescription());
                aspec.setRequiredZoneUuid(msg.getZoneUuid());
                aspec.setRequiredClusterUuid(msg.getClusterUuid());
                aspec.setRequiredHostUuid(msg.getHostUuid());
                aspec.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
                aspec.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
                aspec.setSshUsername(VirtualRouterGlobalConfig.SSH_USERNAME.value());
                aspec.setSshPort(VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class));
                aspec.setAgentPort(agentPort);
                aspec.setAccountUuid(accountUuid);

                String imgBootMode = ImageSystemTags.BOOT_MODE.getTokenByResourceUuid(imgvo.getUuid(), ImageSystemTags.BOOT_MODE_TOKEN);
                if (ImageBootMode.UEFI.toString().equals(imgBootMode)) {
                    aspec.setInherentSystemTags(Arrays.asList(ImageSystemTags.BOOT_MODE.getTag(imgvo.getUuid()), VmSystemTags.MACHINE_TYPE.instantiateTag(map(e(MACHINE_TYPE_TOKEN, VmMachineType.q35.toString())))));
                }

                String haUuid = null;
                if (msg.getSystemTags() != null) {
                    List<String> tags = new ArrayList<>();
                    for (String sysTag : msg.getSystemTags()) {
                        if (ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.isMatch(sysTag)) {
                            Map<String, String> token = TagUtils.parse(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.getTagFormat(), sysTag);
                            haUuid = token.get(ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN);
                        }
                        if (MevocoVmSystemTags.VM_CPU_PINNING.isMatch(sysTag)) {
                            tags.add(sysTag);
                        }
               	    }

                    if (!tags.isEmpty()) {
                        aspec.setNonInherentSystemTags(tags);
                    }
                    aspec.setStaticIp(ApplianceVmOperator.parseStaticIpSystemTag(msg.getSystemTags()));
                    aspec.setStaticVip(ApplianceVmOperator.parseStaticVipSystemTag(msg.getSystemTags()));
                }

                L3NetworkInventory mgmtNw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getManagementNetworkUuid(), L3NetworkVO.class));


                // NOTE: don't open 22 port here; 22 port is default opened on mgmt network in virtual router with restricted rules
                // open 22 here will cause a non-restricted rule to be added
                String mgmtNwUuid = mgmtNw.getUuid();
                openFirewall(aspec, mgmtNwUuid, agentPort, ApplianceVmFirewallProtocol.tcp);
                openAdditionalPorts(aspec, mgmtNwUuid);

                L3NetworkInventory pnw = null;
                if (offering.getPublicNetworkUuid() != null) {
                    pnw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getPublicNetworkUuid(), L3NetworkVO.class));
                }

                if (haUuid != null) {
                    /* ha group has special public config */
                    VirtualRouterOperator.addVirtualRouterVmNicSpec(aspec, mgmtNw, pnw, new ArrayList<>(), new ArrayList<>(), true, true);
                    for (ApplianceVmHaExtensionPoint ext : pluginRgty.getExtensionList(ApplianceVmHaExtensionPoint.class)) {
                        ext.createApplianceVmHaSpec(aspec, offering.getUuid(), haUuid);
                    }
                } else {
                    VirtualRouterOperator.addVirtualRouterVmNicSpec(aspec, mgmtNw, pnw, new ArrayList<>(), new ArrayList<>(), true, false);
                }

                final String fHaUuid = haUuid;
                apvmf.createApplianceVm(aspec, new ReturnValueCompletion<ApplianceVmInventory>(completion) {
                    @Override
                    public void success(ApplianceVmInventory apvm) {
                        String paraDegree = VirtualRouterSystemTags.VR_OFFERING_PARALLELISM_DEGREE.getTokenByResourceUuid(offering.getUuid(), VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN);

                        if (paraDegree != null) {
                            SystemTagCreator creator = VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.newSystemTagCreator(apvm.getUuid());
                            creator.setTagByTokens(map(e(
                                    VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN,
                                    paraDegree
                            )));
                            creator.create();
                        }

                        if (fHaUuid != null) {
                            SystemTagCreator creator = ApplianceVmSystemTags.APPLIANCEVM_HA_UUID.newSystemTagCreator(apvm.getUuid());
                            creator.setTagByTokens(map(e(
                                    ApplianceVmSystemTags.APPLIANCEVM_HA_UUID_TOKEN,
                                    fHaUuid
                            )));
                            creator.create();
                        }

                        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class));
                        snatProxy.attachNetworkService(apvm.getUuid(), NetworkServiceType.SNAT.toString(),
                                asList(vrInv.getDefaultRouteL3NetworkUuid()));

                        completion.success(vrInv);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (fHaUuid != null) {
                            Iterator<ApplianceVmHaExtensionPoint> exps = pluginRgty.getExtensionList(ApplianceVmHaExtensionPoint.class).iterator();
                            createApplianceVmHaSpecRollBack(exps, aspec, offering.getUuid(), fHaUuid, new NoErrorCompletion(completion) {
                                @Override
                                public void done() {
                                    failAndReply(errorCode);
                                }
                            });
                        } else {
                            failAndReply(errorCode);
                        }

                    }
                });
            }
        }

        new newVirtualRouterJob().create();

    }

    @Override
    public void beforeAcquireVirtualRouterVmExtensionPoint(VirtualRouterStruct struct) {
        if (!VpcConstants.VPC_L3_NETWORK_TYPE.equals(struct.getL3Network().getType())) {
            return;
        }

        if (struct.getInherentSystemTags() != null
                && struct.getInherentSystemTags().contains(VirtualRouterSystemTags.DEDICATED_ROLE_VR.getTagFormat())
                && struct.getInherentSystemTags().contains(VirtualRouterSystemTags.VR_LB_ROLE.getTagFormat())) {
            return;
        }
        String sql = "select vr from VirtualRouterVmVO vr, VmNicVO nic where vr.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("l3Uuid", struct.getL3Network().getUuid());
        q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        List<VirtualRouterVmVO> vrs = q.getResultList();
        if (vrs == null || vrs.isEmpty()) {
            throw new OperationFailureException(operr("vpc l3 network must attach a vpc vrouter first before " +
                    "do anything related to vrouter(like start/stop vm, create lb, etc.)"));
        }
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "deploy_zstack_network_on_host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    trigger.next();
                    return;
                }

                SshFileMd5Checker checker = new SshFileMd5Checker();
                KVMHostInventory inv = context.getInventory();
                String hostname = inv.getManagementIp();
                Integer port = inv.getSshPort();
                String username = inv.getUsername();
                String password = inv.getPassword();
                String hostuuid = inv.getUuid();

                checker.setTargetIp(hostname);
                checker.setUsername(username);
                checker.setPassword(password);
                checker.setSshPort(port);
                String srcAgentName = VpcConstants.AGENT_PACKAGE_NAME;
                if (inv.getArchitecture() != null && !HostConstant.HOST_ARCHITECTURE_X86_64.equals(inv.getArchitecture())) {
                    srcAgentName = srcAgentName.replace("bin", inv.getArchitecture()+ ".bin");
                }
                checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("%s/%s", VpcConstants.ANSIBLE_MODULE_PATH, srcAgentName), true).getAbsolutePath(),
                        String.format("/var/lib/zstack/zsn-agent/package/%s", VpcConstants.AGENT_PACKAGE_NAME));

                AnsibleRunner runner = new AnsibleRunner();
                runner.setAgentPort(VpcGlobalProperty.AGENT_PORT);
                runner.installChecker(checker);
                runner.setPassword(password);
                runner.setUsername(username);
                runner.setTargetIp(hostname);
                runner.setTargetUuid(inv.getUuid());
                runner.setSshPort(port);
                runner.setPlayBookName(VpcConstants.ANSIBLE_PLAYBOOK_NAME);

                VpcDeployArguments deployArguments = new VpcDeployArguments();
                deployArguments.setTimeout(VpcGlobalConfig.ZSNP_TMOUT.value(Long.class));
                if (!timeoutMap.getOrDefault(hostuuid, "").equals(VpcGlobalConfig.ZSNP_TMOUT.value())) {
                    timeoutMap.put(hostuuid, VpcGlobalConfig.ZSNP_TMOUT.value());
                    runner.setForceRun(true);
                }

                runner.setDeployArguments(deployArguments);
                runner.run(new ReturnValueCompletion<Boolean>(trigger) {
                    @Override
                    public void success(Boolean deployed) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<VmNicInventory> filterVmNicsForEipInVirtualRouter(VipInventory vip, List<VmNicInventory> candidates) {
        // Note(WeiW): vip in vpc performs just like vrouter mostly, but vpc vr will not start automatically, it's the difference
        if (vip.getPeerL3NetworkUuids() == null || vip.getPeerL3NetworkUuids().isEmpty()) {
            return candidates;
        }

        NetworkServiceProviderType providerType = nwServiceMgr.
                getTypeOfNetworkServiceProviderForService(vip.getPeerL3NetworkUuids().get(0), EipConstant.EIP_TYPE);
        if (!providerType.toString().equals(VYOS_ROUTER_PROVIDER_TYPE) &&
                !providerType.toString().equals(VIRTUAL_ROUTER_PROVIDER_TYPE)) {
            return candidates;
        }

        L3NetworkVO l3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, vip.getPeerL3NetworkUuids().get(0)).find();
        if (!l3.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE)) {
            return candidates;
        }

        List<String> vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), vip.getUuid());
        if ((vrUuids == null || vrUuids.isEmpty()) &&
                !Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3.getUuid()).eq(VmNicVO_.metaData, GUEST_NIC_MASK).isExists()) {
            logger.debug(String.format("There are no vr associate with this vpc vip[uuid: %s], will return empty", vip.getUuid()));
            return new ArrayList<VmNicInventory>();
        }

        return candidates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VmNicInventory> getCandidateVmNicsForLoadBalancerInVirtualRouter(APIGetCandidateVmNicsForLoadBalancerMsg msg, List<VmNicInventory> candidates) {
        List<L3NetworkVO> peerL3NetworkVOs = SQL.New("select l3 " +
                "from L3NetworkVO l3, VipPeerL3NetworkRefVO peer, VipVO vip, LoadBalancerVO lb " +
                "where lb.vipUuid = vip.uuid " +
                "and vip.uuid = peer.vipUuid " +
                "and peer.l3NetworkUuid = l3.uuid " +
                "and lb.uuid = :lbUuid")
                .param("lbUuid", msg.getLoadBalancerUuid())
                .list();

        List<String> peerL3NetworkUuids = peerL3NetworkVOs.stream()
                .map(ResourceVO::getUuid)
                .collect(Collectors.toList());

        if (peerL3NetworkVOs.stream()
                .noneMatch( l3 -> l3.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE))) {
            return candidates;
        }

        Set<String> vrUuids = new HashSet<>(
                Q.New(VmNicVO.class)
                .select(VmNicVO_.vmInstanceUuid)
                .in(VmNicVO_.l3NetworkUuid, peerL3NetworkVOs.stream()
                        .map(ResourceVO::getUuid).collect(Collectors.toList()))
                .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                .listValues());

        String log = String.format("vip of loadbalancer[uuid:%s] has peer l3 networks[uuids: %s], " +
                        "l3 networks attached vrs[uuids:%s]",
                        msg.getLoadBalancerUuid(), peerL3NetworkUuids, vrUuids);
        logger.debug(log);

        DebugUtils.Assert(vrUuids.size() <= 2, log);

        if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(msg.getLoadBalancerUuid())) {
            return candidates;
        }

        if (vrUuids.size() == 0) {
            // Vpc vr has been deleted
            return new ArrayList<VmNicInventory>();
        }

        return candidates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<L3NetworkInventory> getPeerL3NetworksForLoadBalancer(String lbUuid, List<L3NetworkInventory> candidates) {
        List<L3NetworkVO> peerL3NetworkVOs = SQL.New("select l3 " +
                "from L3NetworkVO l3, VipPeerL3NetworkRefVO peer, VipVO vip, LoadBalancerVO lb " +
                "where lb.vipUuid = vip.uuid " +
                "and vip.uuid = peer.vipUuid " +
                "and peer.l3NetworkUuid = l3.uuid " +
                "and lb.uuid = :lbUuid")
                                                .param("lbUuid", lbUuid)
                                                .list();

        List<String> peerL3NetworkUuids = peerL3NetworkVOs.stream()
                                                          .map( l3 -> l3.getUuid())
                                                          .collect(Collectors.toList());

        if (peerL3NetworkVOs.stream()
                            .noneMatch( l3 -> l3.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE))) {
            return candidates;
        }

        Set<String> vrUuids = new HashSet<>(
                Q.New(VmNicVO.class)
                 .select(VmNicVO_.vmInstanceUuid)
                 .in(VmNicVO_.l3NetworkUuid, peerL3NetworkVOs.stream()
                                                             .map(l3 -> l3.getUuid()).collect(Collectors.toList()))
                 .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                 .listValues());

        String log = String.format("vip of loadbalancer[uuid:%s] has peer l3 networks[uuids: %s], " +
                        "l3 networks attached vrs[uuids:%s]",
                lbUuid, peerL3NetworkUuids, vrUuids);
        logger.debug(log);

        DebugUtils.Assert(vrUuids.size() <= 2, log);

        if (LoadBalancerSystemTags.SEPARATE_VR.hasTag(lbUuid)) {
            return candidates;
        }

        if (vrUuids.size() == 0) {
            // Vpc vr has been deleted
            return new ArrayList<L3NetworkInventory>();
        }

        return candidates;
    }

    private Flow createSyncFlow() {
        return new VpcVyosDeployZsnAgentFlow();
    }

    @Override
    public Flow vyosPostStartFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostCreateFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostRebootFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostReconnectFlow() {
        return createSyncFlow();
    }

    private void populateExtensions() {
    }

    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(VpcConstants.ANSIBLE_MODULE_PATH, VpcConstants.ANSIBLE_PLAYBOOK_NAME);
    }

    @Override
    public boolean start() {
        deployAnsible();
        populateExtensions();

        evtf.onLocal(L3NetworkConstant.VROUTER_CREATE_EVENT_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                L3NetworkConstant.VRouterData d = (L3NetworkConstant.VRouterData) data;
                ResourceConfig vpcStrategy = rcf.getResourceConfig(HaGlobalConfig.VM_HA_STRATEGY.getIdentity());
                vpcStrategy.updateValue(d.vrouterUuid, SelfFencerStrategy.Force.toString());
            }
        });

        return true;
    }
    
    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VpcConstants.SERVICE_ID);
    }

    private void handle(APIAddDnsToVpcRouterMsg msg) {
        APIAddDnsToVpcRouterEvent evt = new APIAddDnsToVpcRouterEvent(msg.getId());
        List<String> dns = getAllDnsFromVpcRouter(msg.getUuid());
        if (NetworkUtils.isIpv4Address(msg.getDns())) {
            if (dns.contains(msg.getDns())) {
                evt.setError(operr("dns address [%s] has bean added to vpc router [uuid:%s]",
                        msg.getDns(), msg.getVpcRouterUuid()));
                bus.publish(evt);
                return;
            }
        } else {
            for (String d : dns) {
                if (IPv6NetworkUtils.isIpv6Address(d)) {
                    if (IPv6Address.fromString(msg.getDns()).toBigInteger().equals(IPv6Address.fromString(d).toBigInteger())) {
                        evt.setError(operr("dns address [%s] has bean added to vpc router [uuid:%s]",
                                msg.getDns(), msg.getVpcRouterUuid()));
                        bus.publish(evt);
                        return;
                    }
                }
            }
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("add-dns-%s-to-vpcrouter-%s", msg.getDns(), msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-dns-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        attachDnsToVpcRouter(msg.getUuid(), msg.getDns());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        detachDnsFromVpcRouter(msg.getUuid(), msg.getDns());
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        dnsBackend.applyDnsToVpcRouter(msg.getUuid(), true, new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VpcRouterVmInventory vrInv = VpcRouterVmInventory.valueOf(dbf.findByUuid(msg.getUuid(), VpcRouterVmVO.class));
                        evt.setInventory(vrInv);
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIRemoveDnsFromVpcRouterMsg msg) {
        APIRemoveDnsFromVpcRouterEvent evt = new APIRemoveDnsFromVpcRouterEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-dns-%s-to-vpcrouter-%s", msg.getDns(), msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "write-dns-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        detachDnsFromVpcRouter(msg.getUuid(), msg.getDns());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        attachDnsToVpcRouter(msg.getUuid(), msg.getDns());
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        dnsBackend.applyDnsToVpcRouter(msg.getUuid(), true, new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VpcRouterVmInventory vrInv = VpcRouterVmInventory.valueOf(dbf.findByUuid(msg.getUuid(), VpcRouterVmVO.class));
                        evt.setInventory(vrInv);
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }


    void attachDnsToVpcRouter(String vrUuid, String dnsIp) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVO.isHaEnabled()) {
            VpcRouterDnsVO dnsvo = new VpcRouterDnsVO();
            dnsvo.setDns(dnsIp);
            dnsvo.setVpcRouterUuid(vrUuid);
            dbf.persist(dnsvo);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.attachNetworkServiceToHaRouter(VpcRouterDnsVO.class.getSimpleName(),asList(dnsIp), vrUuid);
            }
        }
    }

    void detachDnsFromVpcRouter(String vrUuid, String dnsIp) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVO.isHaEnabled()) {
            SQL.New(VpcRouterDnsVO.class).eq(VpcRouterDnsVO_.vpcRouterUuid, vrUuid).eq(VpcRouterDnsVO_.dns, dnsIp).delete();
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.detachNetworkServiceFromHaRouter(VpcRouterDnsVO.class.getSimpleName(),asList(dnsIp), vrUuid);
            }
        }
    }

    @Override
    public List<String> getAllDnsFromVpcRouter(String vrUuid) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVO.isHaEnabled()) {
            return Q.New(VpcRouterDnsVO.class).eq(VpcRouterDnsVO_.vpcRouterUuid, vrUuid).select(VpcRouterDnsVO_.dns)
                    .orderBy(VpcRouterDnsVO_.id, SimpleQuery.Od.ASC).listValues();
        } else {
            List<VirtualRouterHaGroupExtensionPoint> exp = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
            if (exp == null || exp.isEmpty()) {
                return new ArrayList<>();
            }

            return exp.get(0).getNetworkServicesFromHaVrUuid(VpcRouterDnsVO.class.getSimpleName(),  vrUuid);
        }
    }

    @Override
    public List<String> getDnsAddress(L3NetworkInventory inv) {
        List<String> dns = new ArrayList<>();

        if (!inv.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE)) {
            return dns;
        }

        List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(inv);
        if (iprs.isEmpty()) {
            return dns;
        }

        List<IpRangeInventory> iprs4 = iprs.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        if (!iprs4.isEmpty()) {
            dns.add(iprs4.get(0).getGateway());
        }

        List<IpRangeInventory> iprs6 = iprs.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());
        if (!iprs6.isEmpty()) {
            dns.add(iprs6.get(0).getGateway());
        }

        return dns;
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct ds = new VirtualRouterHaCallbackStruct();
        ds.type = APPLY_DISTRIBUTEROUTING_TASK;
        ds.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();
                if (vrVO == null) {
                    logger.debug(String.format("ha router[%s] does not exist, can not apply distributed routing", vrUuid));
                    completion.success();
                    return;
                }

                SetDistributedRoutingCmd ccmd = JSONObjectUtil.toObject(task.getJsonData(), SetDistributedRoutingCmd.class);
                applyVpcVRouterDistributedRoutingToRouter(VirtualRouterVmInventory.valueOf(vrVO), ccmd, new ReturnValueCompletion<String>(completion){
                    @Override
                    public void success(String returnValue) {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        };
        structs.add(ds);

        VirtualRouterHaCallbackStruct setNat = new VirtualRouterHaCallbackStruct();
        setNat.type = SET_NAT_TASK;
        setNat.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();
                if (vrVO == null) {
                    logger.debug(String.format("ha router[%s] does not exist, can not set snat", vrUuid));
                    completion.success();
                    return;
                }

                SetSnatStateTaskData d = JSONObjectUtil.toObject(task.getJsonData(), SetSnatStateTaskData.class);
                VmNicInventory masterPublicNic = d.getPublicNic();
                boolean state = d.isState();
                VmNicInventory publicNic = null;
                for (VmNicVO nic : vrVO.getVmNics()) {
                    if (nic.getL3NetworkUuid().equals(masterPublicNic.getL3NetworkUuid())) {
                        publicNic = VmNicInventory.valueOf(nic);
                        break;
                    }
                }
                if (publicNic == null) {
                    logger.error(String.format("can not found nic with ip address [%s] on backup router [uuid:%s]",
                            masterPublicNic.getIp(), vrUuid));
                    completion.success();
                    return;
                }

                /* for management and public is same case, snat ip should use the ip of public vip, not the nic ip */
                publicNic.setIp(masterPublicNic.getIp());
                for (UsedIpInventory ip : publicNic.getUsedIps()) {
                    if (ip.getIpVersion() == IPv6Constants.IPv4) {
                        ip.setIp(masterPublicNic.getIp());
                    }
                }

                setSnatStateToVirtualRouter(VirtualRouterVmInventory.valueOf(vrVO), publicNic, state,
                        new ReturnValueCompletion<String>(completion) {
                            @Override
                            public void success(String returnValue) {
                                completion.success();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.fail(errorCode);
                            }
                        });
            }
        };
        structs.add(setNat);

        return structs;
    }

    @Override
    public void syncVersionToDb(String vrUuid, String version) {
    }
}
