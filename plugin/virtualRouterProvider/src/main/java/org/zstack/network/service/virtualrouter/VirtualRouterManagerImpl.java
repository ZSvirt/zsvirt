package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.appliancevm.*;
import org.zstack.compute.vm.VmNicExtensionPoint;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.configuration.APIUpdateInstanceOfferingEvent;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingState;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.query.QueryBelongFilter;
import org.zstack.header.tag.*;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageSystemTags;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.l3.L3NetworkSystemTags;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.*;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefInventory;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.lb.LbConfigProxy;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefInventory;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipInventory;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.network.service.virtualrouter.vyos.VyosVersionCheckResult;
import org.zstack.network.service.virtualrouter.vyos.VyosVersionManager;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.compute.vm.VmSystemTags.MACHINE_TYPE_TOKEN;
import static org.zstack.core.Platform.*;
import static org.zstack.network.service.virtualrouter.VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
import static org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData.GUEST_NIC_MASK;
import static org.zstack.network.service.virtualrouter.vyos.VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
import static org.zstack.network.service.virtualrouter.vyos.VyosConstants.VYOS_VM_TYPE;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;
import static org.zstack.utils.VipUseForList.SNAT_NETWORK_SERVICE_TYPE;

public class VirtualRouterManagerImpl extends AbstractService implements VirtualRouterManager,
        PrepareDbInitialValueExtensionPoint, L2NetworkCreateExtensionPoint,
        GlobalApiMessageInterceptor, AddExpandedQueryExtensionPoint, GetCandidateVmNicsForLoadBalancerExtensionPoint,
        GetPeerL3NetworksForLoadBalancerExtensionPoint, FilterVmNicsForEipInVirtualRouterExtensionPoint, ApvmCascadeFilterExtensionPoint, ManagementNodeReadyExtensionPoint,
        VipCleanupExtensionPoint, GetL3NetworkForEipInVirtualRouterExtensionPoint, VirtualRouterHaGetCallbackExtensionPoint, AfterAddIpRangeExtensionPoint, QueryBelongFilter {
    private final static CLogger logger = Utils.getLogger(VirtualRouterManagerImpl.class);

    private final static List<String> supportedL2NetworkTypes = new ArrayList<String>();
    private NetworkServiceProviderInventory virtualRouterProvider;
    private final Map<String, VirtualRouterHypervisorBackend> hypervisorBackends = new HashMap<String, VirtualRouterHypervisorBackend>();
    private final Map<String, Integer> vrParallelismDegrees = new ConcurrentHashMap<String, Integer>();

    private List<String> virtualRouterPostCreateFlows;
    private List<String> virtualRouterPostStartFlows;
    private List<String> virtualRouterPostRebootFlows;
    private List<String> virtualRouterPostDestroyFlows;
    private List<String> virtualRouterReconnectFlows;
    private List<String> virtualRouterProvisionConfigFlows;
    private FlowChainBuilder postCreateFlowsBuilder;
    private FlowChainBuilder postStartFlowsBuilder;
    private FlowChainBuilder postRebootFlowsBuilder;
    private FlowChainBuilder postDestroyFlowsBuilder;
    private FlowChainBuilder reconnectFlowsBuilder;
    private FlowChainBuilder provisionConfigFlowsBuilder;

    private List<VirtualRouterPostCreateFlowExtensionPoint> postCreateFlowExtensionPoints;
    private List<VirtualRouterPostStartFlowExtensionPoint> postStartFlowExtensionPoints;
    private List<VirtualRouterPostRebootFlowExtensionPoint> postRebootFlowExtensionPoints;
    private List<VirtualRouterPostReconnectFlowExtensionPoint> postReconnectFlowExtensionPoints;
    private List<VirtualRouterPostDestroyFlowExtensionPoint> postDestroyFlowExtensionPoints;
    private List<VipGetUsedPortRangeExtensionPoint> vipGetUsedPortRangeExtensionPoints;
    private List<VirtualProvisionConfigFlowExtensionPoint> provisionConfigFlowExtensionPoints;

    static {
        supportedL2NetworkTypes.add(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
        supportedL2NetworkTypes.add(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    }

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VirtualRouterProviderFactory providerFactory;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApplianceVmFacade apvmf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private VyosVersionManager vyosVersionManager;
    @Autowired
    private LbConfigProxy lbProxy;
    @Autowired
    private VipConfigProxy vipProxy;
    @Autowired
    protected VirutalRouterDefaultL3ConfigProxy defaultL3ConfigProxy;
    @Autowired
    private EventFacade evf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    private ApplianceVmFactory apvmFactory;

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
        if (msg instanceof CreateVirtualRouterVmMsg) {
            handle((CreateVirtualRouterVmMsg) msg);
        } else if (msg instanceof CheckVirtualRouterVmVersionMsg) {
            handle((CheckVirtualRouterVmVersionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final CreateVirtualRouterVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-vr-for-l3-%s", msg.getL3Network().getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createVirtualRouter(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void createVirtualRouter(final CreateVirtualRouterVmMsg msg, final NoErrorCompletion completion) {
        final L3NetworkInventory l3Network = msg.getL3Network();
        final VirtualRouterOfferingInventory offering = msg.getOffering();
        final CreateVirtualRouterVmReply reply = new CreateVirtualRouterVmReply();
        final String accountUuid = acntMgr.getOwnerAccountUuidOfResource(l3Network.getUuid());

        class newVirtualRouterJob {
            private void failAndReply(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
                completion.done();
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
                    List<Integer> ports = transformAndRemoveNull(udpPorts, Integer::valueOf);
                    for (int p : ports) {
                        openFirewall(aspec, mgmtNwUuid, p, ApplianceVmFirewallProtocol.udp);
                    }
                }
            }

            private void checkIsIpRangeOverlap(){
                String priStartIp;
                String priEndIp;
                String pubStartIp;
                String pubEndIp;

                L3NetworkVO pubL3Network = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid,msg.getOffering().getPublicNetworkUuid()).find();
                /* virtual router only has ipv4 address */
                List<IpRangeInventory> priIpranges = IpRangeHelper.getNormalIpRanges(l3Network, IPv6Constants.IPv4);
                List<IpRangeInventory> pubIpranges = IpRangeHelper.getNormalIpRanges(pubL3Network, IPv6Constants.IPv4);


                for(IpRangeInventory priIprange : priIpranges){
                    for(IpRangeInventory pubIprange : pubIpranges){

                        priStartIp = priIprange.getStartIp();
                        priEndIp = priIprange.getEndIp();
                        pubStartIp = pubIprange.getStartIp();
                        pubEndIp = pubIprange.getEndIp();

                        if(NetworkUtils.isIpv4RangeOverlap(priStartIp,priEndIp,pubStartIp,pubEndIp)){
                            throw new OperationFailureException(argerr("cannot create virtual Router vm while virtual router network overlaps with private network in ip "));
                        }

                    }
                }

            }
            void create() {
                List<String> neededService = l3Network.getNetworkServiceTypesFromProvider(new Callable<String>() {
                    @Override
                    public String call() {
                        return Q.New(NetworkServiceProviderVO.class)
                                .select(NetworkServiceProviderVO_.uuid)
                                .eq(NetworkServiceProviderVO_.type, msg.getProviderType())
                                .findValue();
                    }
                }.call());

                if (neededService.contains(NetworkServiceType.SNAT.toString()) && offering.getPublicNetworkUuid() == null) {
                    ErrorCode err = err(VirtualRouterErrors.NO_PUBLIC_NETWORK_IN_OFFERING, "L3Network[uuid:%s, name:%s] requires SNAT service, but default virtual router offering[uuid:%s, name:%s] doesn't have a public network", l3Network.getUuid(), l3Network.getName(), offering.getUuid(), offering.getName());
                    logger.warn(err.getDetails());
                    failAndReply(err);
                    return;
                }

                checkIsIpRangeOverlap();

                ImageVO imgvo = dbf.findByUuid(offering.getImageUuid(), ImageVO.class);

                final ApplianceVmSpec aspec = new ApplianceVmSpec();
                aspec.setSyncCreate(false);
                aspec.setTemplate(ImageInventory.valueOf(imgvo));
                aspec.setApplianceVmType(ApplianceVmType.valueOf(msg.getApplianceVmType()));
                aspec.setInstanceOffering(offering);
                aspec.setAccountUuid(accountUuid);
                aspec.setName(String.format("vrouter.l3.%s.%s", l3Network.getName(), l3Network.getUuid().substring(0, 6)));
                aspec.setInherentSystemTags(msg.getInherentSystemTags());
                aspec.setSshUsername(VirtualRouterGlobalConfig.SSH_USERNAME.value());
                aspec.setSshPort(VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class));
                aspec.setAgentPort(msg.getApplianceVmAgentPort());

                List<String> tags = new ArrayList<>();
                if (msg.getSystemTags() != null && !msg.getSystemTags().isEmpty()) {
                    tags.addAll(msg.getSystemTags());
                }
                String imgBootMode = ImageSystemTags.BOOT_MODE.getTokenByResourceUuid(imgvo.getUuid(), ImageSystemTags.BOOT_MODE_TOKEN);
                if (ImageBootMode.UEFI.toString().equals(imgBootMode)) {
                    tags.addAll(Arrays.asList(ImageSystemTags.BOOT_MODE.getTag(imgvo.getUuid()), VmSystemTags.MACHINE_TYPE.instantiateTag(map(e(MACHINE_TYPE_TOKEN, VmMachineType.q35.toString())))));
                }
                if (!tags.isEmpty()) {
                    aspec.setInherentSystemTags(tags);
                }

                L3NetworkInventory mgmtNw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getManagementNetworkUuid(), L3NetworkVO.class));
                ApplianceVmNicSpec mgmtNicSpec = new ApplianceVmNicSpec();
                mgmtNicSpec.setL3NetworkUuid(mgmtNw.getUuid());
                mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.MANAGEMENT_NIC_MASK.toString());
                aspec.setManagementNic(mgmtNicSpec);

                String mgmtNwUuid = mgmtNw.getUuid();
                String pnwUuid;

                // NOTE: don't open 22 port here; 22 port is default opened on mgmt network in virtual router with restricted rules
                // open 22 here will cause a non-restricted rule to be added
                openFirewall(aspec, mgmtNwUuid, VirtualRouterGlobalProperty.AGENT_PORT, ApplianceVmFirewallProtocol.tcp);
                openAdditionalPorts(aspec, mgmtNwUuid);

                if (offering.getPublicNetworkUuid() != null && !offering.getManagementNetworkUuid().equals(offering.getPublicNetworkUuid())) {
                    L3NetworkInventory pnw = L3NetworkInventory.valueOf(dbf.findByUuid(offering.getPublicNetworkUuid(), L3NetworkVO.class));
                    ApplianceVmNicSpec pnicSpec = new ApplianceVmNicSpec();
                    pnicSpec.setL3NetworkUuid(pnw.getUuid());
                    pnicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_NIC_MASK.toString());
                    aspec.getAdditionalNics().add(pnicSpec);
                    pnwUuid = pnicSpec.getL3NetworkUuid();
                    aspec.setDefaultRouteL3Network(pnw);
                    aspec.setDefaultL3Network(pnw);
                } else {
                    // use management nic for both management and public
                    mgmtNicSpec.setMetaData(VirtualRouterNicMetaData.PUBLIC_AND_MANAGEMENT_NIC_MASK.toString());
                    pnwUuid = mgmtNwUuid;
                    aspec.setDefaultRouteL3Network(mgmtNw);
                    aspec.setDefaultL3Network(mgmtNw);
                }


                if (!l3Network.getUuid().equals(mgmtNwUuid) && !l3Network.getUuid().equals(pnwUuid)) {
                    ApplianceVmNicSpec nicSpec = new ApplianceVmNicSpec();
                    nicSpec.setL3NetworkUuid(l3Network.getUuid());
                    if ((L3NetworkSystemTags.ROUTER_INTERFACE_IP.hasTag(l3Network.getUuid()) || neededService.contains(NetworkServiceType.SNAT.toString())) && !msg.isNotGatewayForGuestL3Network()) {
                        DebugUtils.Assert(!IpRangeHelper.getNormalIpRanges(l3Network, IPv6Constants.IPv4).isEmpty(), String.format("how can l3Network[uuid:%s] doesn't have ip range", l3Network.getUuid()));
                        IpRangeInventory ipr = IpRangeHelper.getNormalIpRanges(l3Network, IPv6Constants.IPv4).get(0);
                        nicSpec.setL3NetworkUuid(l3Network.getUuid());
                        nicSpec.setAcquireOnNetwork(false);
                        nicSpec.setNetmask(ipr.getNetmask());
                        nicSpec.setIp(ipr.getGateway());
                        nicSpec.setGateway(ipr.getGateway());
                    }
                    if (L3NetworkSystemTags.ROUTER_INTERFACE_IP.hasTag(l3Network.getUuid())) {
                        nicSpec.setIp(L3NetworkSystemTags.ROUTER_INTERFACE_IP.getTokenByResourceUuid(l3Network.getUuid(), L3NetworkSystemTags.ROUTER_INTERFACE_IP_TOKEN));
                    }
                    aspec.getAdditionalNics().add(nicSpec);
                }

                ApplianceVmNicSpec guestNicSpec = mgmtNicSpec.getL3NetworkUuid().equals(l3Network.getUuid()) ? mgmtNicSpec :
                        CollectionUtils.findOneOrNull(aspec.getAdditionalNics(), arg -> arg.getL3NetworkUuid().equals(l3Network.getUuid()));

                guestNicSpec.setMetaData(guestNicSpec.getMetaData() == null ? GUEST_NIC_MASK.toString()
                        : String.valueOf(Integer.parseInt(guestNicSpec.getMetaData()) | GUEST_NIC_MASK));

                if (neededService.contains(NetworkServiceType.DHCP.toString())) {
                    openFirewall(aspec, l3Network.getUuid(), 68, ApplianceVmFirewallProtocol.udp);
                    openFirewall(aspec, l3Network.getUuid(), 67, ApplianceVmFirewallProtocol.udp);
                }
                if (neededService.contains(NetworkServiceType.DNS.toString())) {
                    openFirewall(aspec, l3Network.getUuid(), 53, ApplianceVmFirewallProtocol.udp);
                }

                logger.debug(String.format("unable to find running virtual for L3Network[name:%s, uuid:%s], is about to create a new one",  l3Network.getName(), l3Network.getUuid()));
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

                        reply.setInventory(VirtualRouterVmInventory.valueOf(dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class)));
                        bus.reply(msg, reply);
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        failAndReply(errorCode);
                    }
                });
            }
        }

        new newVirtualRouterJob().create();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateVirtualRouterOfferingMsg) {
            handle((APIUpdateVirtualRouterOfferingMsg) msg);
        } else if (msg instanceof APIGetAttachablePublicL3ForVRouterMsg) {
            handle((APIGetAttachablePublicL3ForVRouterMsg) msg);
        } else if (msg instanceof APIGetVipUsedPortsMsg) {
            handle((APIGetVipUsedPortsMsg) msg);
        }else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private List<String> getVipUsedPortList(String vipUuid, String protocol){
        List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, vipUuid).listValues();
        VipUseForList useForList = new VipUseForList(useFor);

        List<RangeSet.Range> portRangeList = new ArrayList<RangeSet.Range>();
        for (VipGetUsedPortRangeExtensionPoint ext : vipGetUsedPortRangeExtensionPoints) {
            RangeSet range = ext.getVipUsePortRange(vipUuid, protocol, useForList);
            portRangeList.addAll(range.getRanges());
        }

        RangeSet portRange = new RangeSet();
        portRange.setRanges(portRangeList);
        return portRange.sortAndToString();
    }

    private void handle(APIGetVipUsedPortsMsg msg) {
        String vipUuid = msg.getUuid();
        String protocl = msg.getProtocol().toUpperCase();

        APIGetVipUsedPortsReply reply = new APIGetVipUsedPortsReply();
        APIGetVipUsedPortsReply.VipPortRangeInventory inv = new APIGetVipUsedPortsReply.VipPortRangeInventory();
        inv.setUuid(vipUuid);
        inv.setProtocol(protocl);
        inv.setUsedPorts(getVipUsedPortList(vipUuid, protocl));
        reply.setInventories(Arrays.asList(inv));
        bus.reply(msg, reply);
    }

    private void handle(APIGetAttachablePublicL3ForVRouterMsg msg) {
        APIGetAttachablePublicL3ForVRouterReply reply = new APIGetAttachablePublicL3ForVRouterReply();
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid()).list();
        List<String> nicL3Uuids = vmNicVOS.stream().map(VmNicVO::getL3NetworkUuid).collect(Collectors.toList());
        List<L3NetworkVO> l3NetworkVOS = Q.New(L3NetworkVO.class).notEq(L3NetworkVO_.category, L3NetworkCategory.Private)
                .notIn(L3NetworkVO_.uuid, nicL3Uuids).list();

        if (l3NetworkVOS == null || l3NetworkVOS.isEmpty()) {
            reply.setInventories(new ArrayList<L3NetworkInventory>());
            bus.reply(msg, reply);
            return;
        }

        Set<L3NetworkVO> attachableL3NetworkVOS = new HashSet<>(l3NetworkVOS);

        for (L3NetworkVO l3NetworkVO : l3NetworkVOS) {
            List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(l3NetworkVO, IPv6Constants.IPv4);
            List<IpRangeInventory> ip6rs = IpRangeHelper.getNormalIpRanges(l3NetworkVO, IPv6Constants.IPv6);
            if (iprs.isEmpty() && ip6rs.isEmpty()) {
                attachableL3NetworkVOS.remove(l3NetworkVO);
            }

            for (VmNicVO vmNicVO : vmNicVOS) {
                for (UsedIpVO ipVO : vmNicVO.getUsedIps()) {
                    NormalIpRangeVO ipRangeVO = dbf.findByUuid(ipVO.getIpRangeUuid(), NormalIpRangeVO.class);
                    if (ipRangeVO.getIpVersion() == IPv6Constants.IPv4 && !iprs.isEmpty()) {
                        if (NetworkUtils.isCidrOverlap(ipRangeVO.getNetworkCidr(), iprs.get(0).getNetworkCidr())) {
                            attachableL3NetworkVOS.remove(l3NetworkVO);
                        }
                    } else if (ipRangeVO.getIpVersion() == IPv6Constants.IPv6 && !ip6rs.isEmpty()) {
                        if (IPv6NetworkUtils.isIpv6RangeOverlap(ipRangeVO.getStartIp(), ipRangeVO.getEndIp(),
                                ip6rs.get(0).getStartIp(), ip6rs.get(0).getEndIp())) {
                            attachableL3NetworkVOS.remove(l3NetworkVO);
                        }
                    }
                }
            }
        }
        reply.setInventories(L3NetworkInventory.valueOf(attachableL3NetworkVOS));
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateVirtualRouterOfferingMsg msg) {
        VirtualRouterOfferingVO ovo = dbf.findByUuid(msg.getUuid(), VirtualRouterOfferingVO.class);
        boolean updated = false;
        if (msg.getName() != null) {
            ovo.setName(msg.getName());
            updated = true;
        }
        if (msg.getDescription() != null) {
            ovo.setDescription(msg.getDescription());
            updated = true;
        }
        if (msg.getImageUuid() != null) {
            ovo.setImageUuid(msg.getImageUuid());
            updated = true;
        }

        if (updated) {
            ovo = dbf.updateAndRefresh(ovo);
        }

        if (msg.getIsDefault() != null) {
            DefaultVirtualRouterOfferingSelector selector = new DefaultVirtualRouterOfferingSelector();
            selector.setZoneUuid(ovo.getZoneUuid());
            selector.setPreferToBeDefault(msg.getIsDefault());
            selector.setOfferingUuid(ovo.getUuid());
            selector.selectDefaultOffering();
        }

        APIUpdateInstanceOfferingEvent evt = new APIUpdateInstanceOfferingEvent(msg.getId());
        evt.setInventory(VirtualRouterOfferingInventory.valueOf(dbf.reload(ovo)));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VirtualRouterConstant.SERVICE_ID);
    }

    private void rebootVirtualRouterVmOnRebootEvent() {
        evf.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_REBOOT, new EventCallback<Object>() {
            @Override
            protected void run(Map<String, String> tokens, Object data) {
                String vmUuid = (String) data;
                VirtualRouterVmVO vrVO = dbf.findByUuid(vmUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    return;
                }

                if (!destMaker.isManagedByUs(vmUuid)) {
                    return;
                }

                RebootVmInstanceMsg msg = new RebootVmInstanceMsg();
                msg.setVmInstanceUuid(vmUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                bus.send(msg);
            }
        });
    }

    private void installConfigValidateExtension() {
        VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                String[] ports = value.split("-");
                if (ports != null && ports.length == 2) {
                    try {
                        long lowPort = Long.parseLong(ports[0]);
                        long upPort = Long.parseLong(ports[1]);
                        if  (!( (lowPort >= 1024 && upPort <= 65535 && lowPort <= upPort) || (lowPort == 0 && upPort ==0) )) {
                            throw new GlobalConfigException(String.format("can not update %s:[%s,%s],beacause %s must in range in [1024, 65535],",
                                    VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.getName(), ports[0],ports[1],VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.getName()));
                        }
                    } catch (NumberFormatException e) {
                        throw new GlobalConfigException(String.format("%s %s is not a number or out of range of a Long type", ports[0], ports[1]), e);
                    }
                } else {
                    throw new GlobalConfigException(String.format("%s must be this format port-port",
                            VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.getName()));
                }
            }
        });
    }

    @Override
    public boolean start() {
        populateExtensions();
        deployAnsible();
        buildWorkFlowBuilder();
        installSystemValidator();
        installConfigValidateExtension();
        rebootVirtualRouterVmOnRebootEvent();
        setupCanonicalEvents();

        VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                if (VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.isMatch(tag.getTag())) {
                    String value = VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.getTokenByTag(tag.getTag(), VirtualRouterSystemTags.PARALLELISM_DEGREE_TOKEN);
                    vrParallelismDegrees.put(tag.getResourceUuid(), Integer.valueOf(value));
                }
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                if (VirtualRouterSystemTags.VR_PARALLELISM_DEGREE.isMatch(tag.getTag())) {
                    vrParallelismDegrees.remove(tag.getResourceUuid());
                }
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
            }
        });

        upgradeVirtualRouterImageType();
        return true;
    }

    private void setupCanonicalEvents() {
        evf.on(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {

            @Override
            protected void run(Map tokens, Object data) {
                VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
                if (VmInstanceState.Paused.toString().equals(d.getNewState())
                        || VmInstanceState.NoState.toString().equals(d.getNewState())) {
                    ApplianceVmVO applianceVmVO = Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, d.getInventory().getUuid()).find();
                    if (applianceVmVO == null) {
                        return;
                    }

                    if (VmInstanceState.Paused.toString().equals(d.getNewState())) {
                        ApplianceVmCanonicalEvents.ApplianceVmStateChangeData applianceVmStateChangeData = new ApplianceVmCanonicalEvents.ApplianceVmStateChangeData();
                        applianceVmStateChangeData.setOldState(d.getOldState());
                        applianceVmStateChangeData.setNewState(d.getNewState());
                        applianceVmStateChangeData.setApplianceVmUuid(d.getVmUuid());
                        applianceVmStateChangeData.setInv(ApplianceVmInventory.valueOf(applianceVmVO));
                        evf.fire(ApplianceVmCanonicalEvents.APPLIANCEVM_STATE_CHANGED_PATH, applianceVmStateChangeData);
                    }

                    if (VmInstanceState.NoState.toString().equals(d.getNewState())) {
                        logger.debug(String.format("the virtual router vm[uuid: %s] is in NoState, stop it anyway", d.getInventory().getUuid()));
                        // NoState means we lost control of the vm, stop the vm and wait vm ha to start it
                        // Do not use reboot to make sure vm could be ha to another host.
                        // Only tries to stop vm once, if it failed, let ha to handle it.
                        StopVmInstanceMsg msg = new StopVmInstanceMsg();
                        msg.setVmInstanceUuid(d.getInventory().getUuid());
                        msg.setGcOnFailure(true);
                        msg.setType(StopVmType.force.toString());
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, d.getInventory().getUuid());
                        bus.send(msg);
                    }
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void upgradeVirtualRouterImageType() {
        if (!LoadBalancerGlobalProperty.UPGRADE_LB_SERVER_GROUP) {
            return;
        }

        List<String> imageUuids = Q.New(ImageVO.class).eq(ImageVO_.system, true).select(ImageVO_.uuid).listValues();
        if (imageUuids.isEmpty()) {
            return;
        }

        for (String uuid : imageUuids) {
            /* there are 1 kinds of system image, vyos router image */
            String applianceType = ImageSystemTags.APPLIANCE_IMAGE_TYPE.getTokenByResourceUuid(uuid,
                    ImageSystemTags.APPLIANCE_IMAGE_TYPE_TOKEN);
            if (applianceType == null) {
                SystemTagCreator creator = ImageSystemTags.APPLIANCE_IMAGE_TYPE.newSystemTagCreator(uuid);
                creator.setTagByTokens(map(e(ImageSystemTags.APPLIANCE_IMAGE_TYPE_TOKEN, VYOS_VM_TYPE)));
                creator.inherent = true;
                creator.create();
            }
        }
    }

    private void installSystemValidator() {
        class VirtualRouterOfferingValidator implements SystemTagCreateMessageValidator, SystemTagValidator {

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg instanceof APICreateL3NetworkMsg) {
                    validate((APICreateL3NetworkMsg) msg);
                }

            }

            private void validate(APICreateL3NetworkMsg msg) {
                for (String sysTag : msg.getSystemTags()) {
                    if (VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING.isMatch(sysTag)) {
                        validateVirtualRouterOffering(sysTag, msg.getResourceUuid());
                    }
                }
            }

            private void validateVirtualRouterOffering(String sysTag, String resourceUuid) {
                String offeringUuid = VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING.getTokenByTag(sysTag, VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING_TOKEN);
                VirtualRouterOfferingVO offeringVO = dbf.findByUuid(offeringUuid, VirtualRouterOfferingVO.class);
                if (offeringVO == null) {
                    throw new ApiMessageInterceptionException(argerr("No virtual router instance offering with uuid:%s is found", offeringUuid));
                }

                if (resourceUuid != null && resourceUuid.equals(offeringVO.getPublicNetworkUuid())) {
                    throw new ApiMessageInterceptionException(argerr("the network of virtual router instance offering with uuid:%s can't be same with private l3 network uuid:%s", offeringUuid, resourceUuid));
                }
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING.isMatch(systemTag)) {
                    validateVirtualRouterOffering(systemTag, resourceUuid);
                }
            }
        }

        class VirtualRouterOfferingOperator implements SystemTagResourceDeletionOperator {
            @Override
            public void execute(Collection resourceUuids) {
                List<String> tags = (List<String>) resourceUuids.stream()
                        .map(resourceUuid -> TagUtils.tagPatternToSqlPattern(VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING.instantiateTag(
                                map(e(VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING_TOKEN, resourceUuid)))))
                        .collect(Collectors.toList());

                SQL.New(SystemTagVO.class).in(SystemTagVO_.tag, tags).delete();
            }
        }

        VirtualRouterOfferingValidator validator = new VirtualRouterOfferingValidator();
        tagMgr.installCreateMessageValidator(L3NetworkVO.class.getSimpleName(), validator);
        tagMgr.installAfterResourceDeletionOperator(InstanceOfferingVO.class.getSimpleName(), new VirtualRouterOfferingOperator());
        VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING.installValidator(validator);
    }


    @Transactional
    private void prepareDbInitialValueForPublicVip() {
        /* vip upgrade for multiple public interface */
        List<VipVO> vips = new ArrayList<>();
        List<VirtualRouterVipVO> vvips = new ArrayList<>();
        List<VirtualRouterVmVO> vrVos = Q.New(VirtualRouterVmVO.class).list();
        for (VirtualRouterVmVO vr : vrVos) {
            if (vr.getDefaultL3NetworkUuid() == null) {
                SQL.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vr.getUuid())
                        .set(VirtualRouterVmVO_.defaultL3NetworkUuid, vr.getPublicNetworkUuid()).update();
            }

            if (vr.isHaEnabled()) {
                continue;
            }

            /* create public vip for additional public nic */
            VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vr);
            for (VmNicInventory nic : vrInv.getAdditionalPublicNics()) {
                if (Q.New(VipVO.class).eq(VipVO_.ip, nic.getIp()).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid()).isExists()) {
                    continue;
                }

                L3NetworkInventory vipL3 = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
                IpRangeInventory ipRange = null;
                for (IpRangeInventory ipr : IpRangeHelper.getNormalIpRanges(vipL3, IPv6Constants.IPv4)) {
                    if (NetworkUtils.isIpv4InRange(nic.getIp(), ipr.getStartIp(), ipr.getEndIp())) {
                        ipRange = ipr;
                        break;
                    }
                }

                if (ipRange == null){
                    logger.warn(String.format("can not find ip range for ip address[ip:%s, l3 network:%s]",
                            nic.getIp(), nic.getL3NetworkUuid()));
                    continue;
                }

                if (nic.getUsedIpUuid() == null) {
                    logger.warn(String.format("usedIpUuid of vmnic [Uuid:%s] is NULL", nic.getUuid()));
                    continue;
                }

                VipVO vipvo = new VipVO();
                vipvo.setUuid(Platform.getUuid());
                vipvo.setName(String.format("vip-%s", vr.getName()));
                vipvo.setDescription(String.format("system vip for %s", vr.getName()));
                vipvo.setState(VipState.Enabled);
                vipvo.setGateway(nic.getGateway());
                vipvo.setIp(nic.getIp());
                vipvo.setIpRangeUuid(ipRange.getUuid());
                vipvo.setL3NetworkUuid(nic.getL3NetworkUuid());
                vipvo.setNetmask(nic.getNetmask());
                vipvo.setUsedIpUuid(nic.getUsedIpUuid());
                vipvo.setAccountUuid(Account.getAccountUuidOfResource(vr.getUuid()));
                vipvo.setSystem(true);
                vipvo.setPrefixLen(ipRange.getPrefixLen());
                vipvo.setServiceProvider(VyosConstants.PROVIDER_TYPE.toString());

                vips.add(vipvo);

                VirtualRouterVipVO vvip = new VirtualRouterVipVO();
                vvip.setVirtualRouterVmUuid(vrInv.getUuid());
                vvip.setUuid(vipvo.getUuid());
                vvips.add(vvip);
            }
        }

        if (!vips.isEmpty()) {
            dbf.persistCollection(vips);
        }

        if (!vvips.isEmpty()) {
            dbf.persistCollection(vvips);
        }
    }

    public void prepareDbInitialValue() {
        prepareDbInitialValueForPublicVip();

        NetworkServiceProviderVO rpvo = Q.New(NetworkServiceProviderVO.class)
                .eq(NetworkServiceProviderVO_.type, VIRTUAL_ROUTER_PROVIDER_TYPE)
                .find();
        if (rpvo != null) {
            virtualRouterProvider = NetworkServiceProviderInventory.valueOf(rpvo);
            return;
        }
        
        NetworkServiceProviderVO vo = new NetworkServiceProviderVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(VIRTUAL_ROUTER_PROVIDER_TYPE);
        vo.setDescription("zstack virtual router network service provider");
        vo.getNetworkServiceTypes().add(NetworkServiceType.DHCP.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.DNS.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.SNAT.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.PortForwarding.toString());
        vo.getNetworkServiceTypes().add(EipConstant.EIP_NETWORK_SERVICE_TYPE);
        vo.getNetworkServiceTypes().add(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        vo.setType(VIRTUAL_ROUTER_PROVIDER_TYPE);
        vo = dbf.persistAndRefresh(vo);
        virtualRouterProvider = NetworkServiceProviderInventory.valueOf(vo);
    }
    
    private void populateExtensions() {
        for (VirtualRouterHypervisorBackend extp : pluginRgty.getExtensionList(VirtualRouterHypervisorBackend.class)) {
            VirtualRouterHypervisorBackend old = hypervisorBackends.get(extp.getVirtualRouterSupportedHypervisorType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VirtualRouterHypervisorBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), old.getVirtualRouterSupportedHypervisorType()));
            }
            hypervisorBackends.put(extp.getVirtualRouterSupportedHypervisorType().toString(), extp);
        }

        postCreateFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostCreateFlowExtensionPoint.class);
        postStartFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostStartFlowExtensionPoint.class);
        postRebootFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostRebootFlowExtensionPoint.class);
        postReconnectFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostReconnectFlowExtensionPoint.class);
        postDestroyFlowExtensionPoints = pluginRgty.getExtensionList(VirtualRouterPostDestroyFlowExtensionPoint.class);
        vipGetUsedPortRangeExtensionPoints = pluginRgty.getExtensionList(VipGetUsedPortRangeExtensionPoint.class);
        provisionConfigFlowExtensionPoints = pluginRgty.getExtensionList(VirtualProvisionConfigFlowExtensionPoint.class);
    }
    
    private NetworkServiceProviderVO getRouterVO() {
        return Q.New(NetworkServiceProviderVO.class)
                .eq(NetworkServiceProviderVO_.type, VIRTUAL_ROUTER_PROVIDER_TYPE)
                .find();
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {
    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
        if (!supportedL2NetworkTypes.contains(l2Network.getType())) {
            return;
        }
        
        NetworkServiceProviderVO vo = getRouterVO();
        NetworkServiceProvider router = providerFactory.getNetworkServiceProvider(vo);
        try {
            router.attachToL2Network(l2Network, null);
        } catch (NetworkException e) {
            String err = String.format("unable to attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s], %s",
                    vo.getUuid(), vo.getName(), vo.getType(), l2Network.getUuid(), l2Network.getName(), l2Network.getType(), e.getMessage());
            logger.warn(err, e);
            return;
        }
        
        NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
        ref.setNetworkServiceProviderUuid(vo.getUuid());
        ref.setL2NetworkUuid(l2Network.getUuid());
        dbf.persist(ref);
        String info = String.format("successfully attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s]",
                vo.getUuid(), vo.getName(), vo.getType(), l2Network.getUuid(), l2Network.getName(), l2Network.getType());
        logger.debug(info);
    }



    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(VirtualRouterConstant.ANSIBLE_MODULE_PATH, VirtualRouterConstant.ANSIBLE_PLAYBOOK_NAME);
    }
    
    @Override
    public VirtualRouterHypervisorBackend getHypervisorBackend(HypervisorType hypervisorType) {
        VirtualRouterHypervisorBackend b = hypervisorBackends.get(hypervisorType.toString());
        if (b == null) {
            throw new CloudRuntimeException(String.format("unable to find VirtualRouterHypervisorBackend for hypervisorType[%s]", hypervisorType));
        }
        return b;
    }

    @Override
    public String buildUrl(String mgmtNicIp, String subPath) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(VirtualRouterGlobalProperty.AGENT_URL_SCHEME);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(mgmtNicIp);
        }

        ub.port(VirtualRouterGlobalProperty.AGENT_PORT);
        if (!"".equals(VirtualRouterGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(VirtualRouterGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);

        return ub.build().toUriString();
    }

    private void buildWorkFlowBuilder() {
        postCreateFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostCreateFlows).construct();
        postStartFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostStartFlows).construct();
        postRebootFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostRebootFlows).construct();
        postDestroyFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterPostDestroyFlows).construct();
        reconnectFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterReconnectFlows).construct();
        provisionConfigFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(virtualRouterProvisionConfigFlows).construct();
    }

    @Override
    public List<String> selectL3NetworksNeedingSpecificNetworkService(List<String> candidate, NetworkServiceType nsType) {
        if (candidate == null || candidate.isEmpty()) {
            return new ArrayList<>(0);
        }

        // need to specify provider type due to that the provider might be Flat
        return SQL.New("select ref.l3NetworkUuid from NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO nspv" +
                " where ref.l3NetworkUuid in (:candidate) and ref.networkServiceType = :stype" +
                " and nspv.uuid = ref.networkServiceProviderUuid and nspv.type in (:ntype)")
                .param("candidate", candidate)
                .param("stype", nsType.toString())
                .param("ntype", asList(VIRTUAL_ROUTER_PROVIDER_TYPE, VYOS_ROUTER_PROVIDER_TYPE))
                .list();
    }

    @Override
    public List<String> selectGuestL3NetworksNeedingSpecificNetworkService(List<String> candidate, NetworkServiceType nsType, String publicUuid) {
        List<String> ret = selectL3NetworksNeedingSpecificNetworkService(candidate, nsType);
        if (ret.isEmpty()) {
            return new ArrayList<>();
        }

        /*get guest L3 includes two parts:
        1. the virtual router all guest networks
        2. flat networks both attached with nsType services and a virtualrouter offer
        */

        /*
        step 1: get the offerings that public network is publicuuid
        step 2: get all the guest networks that has attached with these offerings
         */
        List<String > offeringUuids = Q.New(VirtualRouterOfferingVO.class).eq(VirtualRouterOfferingVO_.publicNetworkUuid, publicUuid)
                                        .eq(VirtualRouterOfferingVO_.state, InstanceOfferingState.Enabled)
                                       .select(VirtualRouterOfferingVO_.uuid).listValues();
        if (offeringUuids.isEmpty()) {
            /*public network is same with that of backend server*/
            if (ret.contains(publicUuid)) {
                return Arrays.asList(publicUuid);
            } else {
                return new ArrayList<>();
            }
        }

        return ret.stream().filter(l3 -> {
            L3NetworkVO l3Vo = dbf.findByUuid(l3, L3NetworkVO.class);
            if ( !l3Vo.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
                return false;
            }
            if (l3Vo.getNetworkServices().stream().anyMatch(service -> VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE.equals(service.getNetworkServiceType()))) {
                /*virtual networks*/
                List<VirtualRouterOfferingInventory> offeringInventories = findOfferingByGuestL3Network(L3NetworkInventory.valueOf(l3Vo));
                if (offeringInventories == null || offeringInventories.isEmpty()) {
                    return false;
                }
                return l3.equals(publicUuid) || offeringInventories.stream().anyMatch(it -> offeringUuids.contains(it.getUuid()));
            } else {
                /*flat private network*/
                /*List<String> offer = Q.New(SystemTagVO.class).
                        eq(SystemTagVO_.tag, VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK.instantiateTag(map(e(VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK_TOKEN, l3))))
                                      .select(SystemTagVO_.resourceUuid).eq(SystemTagVO_.resourceType, InstanceOfferingVO.class.getSimpleName())
                                      .in(SystemTagVO_.resourceUuid, offeringUuids)
                                      .listValues();

                return !offer.isEmpty();*/
                List<String> offer = VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING.getTokensOfTagsByResourceUuid(l3)
                        .stream()
                        .map(tokens -> tokens.get(VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING_TOKEN))
                        .collect(Collectors.toList());
                if (offer.isEmpty()) {
                    return false;
                }

                return l3.equals(publicUuid) || offer.stream().anyMatch(offeringUuids::contains);
            }

        }).collect(Collectors.toList());
    }

    @Override
    public boolean isL3NetworkNeedingNetworkServiceByVirtualRouter(String l3Uuid, String nsType) {
        if (l3Uuid == null) {
            return false;
        }
        // no need to specify provider type, L3 networks identified by candidates are served by virtual router or vyos
        return Q.New(NetworkServiceL3NetworkRefVO.class)
                .eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3Uuid)
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, nsType)
                .isExists();
    }

    @Override
    public boolean isL3NetworksNeedingNetworkServiceByVirtualRouter(List<String> l3Uuids, String nsType) {
        if (l3Uuids == null || l3Uuids.isEmpty()) {
            return false;
        }
        // no need to specify provider type, L3 networks identified by candidates are served by virtual router or vyos
        return Q.New(NetworkServiceL3NetworkRefVO.class)
                .in(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3Uuids)
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, nsType)
                .isExists();
    }

    private void acquireVirtualRouterVmInternal(VirtualRouterStruct struct,  final ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        final L3NetworkInventory l3Nw = struct.getL3Network();
        final VirtualRouterOfferingValidator validator = struct.getOfferingValidator();
        final VirtualRouterVmSelector selector = struct.getVirtualRouterVmSelector();

        VirtualRouterVmInventory vr = new Callable<VirtualRouterVmInventory>() {
            @Transactional(readOnly = true)
            private VirtualRouterVmVO findVR() {
                String sql = "select vr from VirtualRouterVmVO vr, VmNicVO nic where vr.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
                TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
                q.setParameter("l3Uuid", l3Nw.getUuid());
                q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
                List<VirtualRouterVmVO> vrs = q.getResultList();

                if (vrs.isEmpty()) {
                    return null;
                }

                /* if there is master, return master */
                for (VirtualRouterVmVO vo : vrs) {
                    if (ApplianceVmHaStatus.Master == vo.getHaStatus()) {
                        return vo;
                    }
                }

                if (selector == null) {
                    return findTheEarliestOne(vrs);
                } else {
                    return selector.select(vrs);
                }
            }

            private VirtualRouterVmVO findTheEarliestOne(List<VirtualRouterVmVO> vrs) {
                VirtualRouterVmVO vr = null;
                for (VirtualRouterVmVO v : vrs) {
                    if (vr == null) {
                        vr = v;
                        continue;
                    }

                    vr = vr.getCreateDate().before(v.getCreateDate()) ? vr : v;
                }
                return vr;
            }

            @Override
            public VirtualRouterVmInventory call() {
                VirtualRouterVmVO vr = findVR();
                return vr == null ? null : new VirtualRouterVmInventory(vr);
            }
        }.call();

        if (vr != null) {
            new While<>(pluginRgty.getExtensionList(AfterAcquireVirtualRouterExtensionPoint.class)).each((ext, c) -> {
                ext.afterAcquireVirtualRouter(vr, new Completion(c) {
                    @Override
                    public void success() {
                        c.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        c.addError(errorCode);
                        c.done();
                    }
                });
            }).run(new WhileDoneCompletion(completion) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (!errorCodeList.getCauses().isEmpty()) {
                        completion.fail(errorCodeList.getCauses().get(0));
                        return;
                    }

                    VirtualRouterVmVO vo = dbf.findByUuid(vr.getUuid(), VirtualRouterVmVO.class);
                    completion.success(VirtualRouterVmInventory.valueOf(vo));
                }
            });
            return;
        }

        List<VirtualRouterOfferingInventory> offerings = findOfferingByGuestL3Network(l3Nw);
        if (offerings == null) {
            ErrorCode err = err(VirtualRouterErrors.NO_DEFAULT_OFFERING, "unable to find a virtual router offering for l3Network[uuid:%s] in zone[uuid:%s], please at least create a default virtual router offering in that zone",
                    l3Nw.getUuid(), l3Nw.getZoneUuid());
            logger.warn(err.getDetails());
            completion.fail(err);
            return;
        }

        if (struct.getVirtualRouterOfferingSelector() == null) {
            struct.setVirtualRouterOfferingSelector(new VirtualRouterOfferingSelector() {
                @Override
                public VirtualRouterOfferingInventory selectVirtualRouterOffering(L3NetworkInventory l3, List<VirtualRouterOfferingInventory> candidates) {
                    Optional<VirtualRouterOfferingInventory> opt = candidates.stream().filter(VirtualRouterOfferingInventory::isDefault).findAny();
                    return opt.orElseGet(() -> candidates.get(0));
                }
            });
        }

        VirtualRouterOfferingInventory offering = struct.getVirtualRouterOfferingSelector().selectVirtualRouterOffering(l3Nw, offerings);

        if (validator != null) {
            validator.validate(offering);
        }

        CreateVirtualRouterVmMsg msg = new CreateVirtualRouterVmMsg();
        msg.setNotGatewayForGuestL3Network(struct.isNotGatewayForGuestL3Network());
        msg.setL3Network(l3Nw);
        msg.setOffering(offering);
        msg.setInherentSystemTags(struct.getInherentSystemTags());
        msg.setProviderType(struct.getProviderType());
        msg.setApplianceVmType(struct.getApplianceVmType());
        msg.setApplianceVmAgentPort(struct.getApplianceVmAgentPort());

        bus.makeTargetServiceIdByResourceUuid(msg, VirtualRouterConstant.SERVICE_ID, l3Nw.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success(((CreateVirtualRouterVmReply) reply).getInventory());
                }
            }
        });
    }

    @Override
    public void acquireVirtualRouterVm(VirtualRouterStruct struct, final ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        for (BeforeAcquireVirtualRouterVmExtensionPoint extp : pluginRgty.getExtensionList(
                BeforeAcquireVirtualRouterVmExtensionPoint.class)) {
            extp.beforeAcquireVirtualRouterVmExtensionPoint(struct);
        }

        //TODO: find a way to remove the GLock
        String syncName = String.format("glock-vr-l3-%s", struct.getL3Network().getUuid());
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncName;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final GLock lock = new GLock(syncName, TimeUnit.HOURS.toSeconds(1));
                lock.setAlsoUseMemoryLock(false);
                lock.lock();
                acquireVirtualRouterVmInternal(struct, new ReturnValueCompletion<VirtualRouterVmInventory>(chain, completion) {
                    @Override
                    public void success(VirtualRouterVmInventory returnValue) {
                        lock.unlock();
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        lock.unlock();
                        completion.fail(operr("Failed to start vr l3[uuid: %s]", struct.getL3Network().getUuid()).causedBy(errorCode));
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return syncName;
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public VirtualRouterVmInventory getVirtualRouterVm(L3NetworkInventory l3Nw) {
        String sql = "select vm from VirtualRouterVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
        TypedQuery<VirtualRouterVmVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterVmVO.class);
        q.setParameter("l3Uuid", l3Nw.getUuid());
        q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        List<VirtualRouterVmVO> vos = q.getResultList();
        if (vos.isEmpty()) {
            return null;
        }

        for (VirtualRouterVmVO vo : vos) {
            if (ApplianceVmHaStatus.Master == vo.getHaStatus()) {
                return VirtualRouterVmInventory.valueOf(vo);
            }
        }

        return VirtualRouterVmInventory.valueOf(vos.get(0));
    }

    @Override
    public boolean isVirtualRouterRunningForL3Network(String l3Uuid) {
        return countVirtualRouterRunningForL3Network(l3Uuid) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countVirtualRouterRunningForL3Network(String l3Uuid) {
        String sql = "select count(vm) from ApplianceVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and vm.state = :vmState and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("l3Uuid", l3Uuid);
        q.setParameter("vmState", VmInstanceState.Running);
        q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        return q.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVirtualRouterForL3Network(String l3Uuid) {
        String sql = "select vm from ApplianceVmVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.l3NetworkUuid = :l3Uuid and nic.metaData in (:guestMeta)";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("l3Uuid", l3Uuid);
        q.setParameter("guestMeta", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST);
        Long count = q.getSingleResult();
        return count > 0;
    }

    @Transactional(readOnly = true)
    private List<VirtualRouterOfferingInventory> findOfferingByGuestL3Network(L3NetworkInventory guestL3) {
        String sql = "select offering from VirtualRouterOfferingVO offering, SystemTagVO stag where " +
                "offering.uuid = stag.resourceUuid and stag.resourceType = :type and offering.zoneUuid = :zoneUuid and stag.tag = :tag and offering.state = :state";
        TypedQuery<VirtualRouterOfferingVO> q = dbf.getEntityManager().createQuery(sql, VirtualRouterOfferingVO.class);
        q.setParameter("type", InstanceOfferingVO.class.getSimpleName());
        q.setParameter("zoneUuid", guestL3.getZoneUuid());
        q.setParameter("tag", VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK.instantiateTag(map(e(VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK_TOKEN, guestL3.getUuid()))));
        q.setParameter("state", InstanceOfferingState.Enabled);
        List<VirtualRouterOfferingVO> vos = q.getResultList();
        if (!vos.isEmpty()) {
            return VirtualRouterOfferingInventory.valueOf1(vos);
        }

        List<String> offeringUuids = VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING.getTokensOfTagsByResourceUuid(guestL3.getUuid())
                .stream().map(tokens -> tokens.get(VirtualRouterSystemTags.VIRTUAL_ROUTER_OFFERING_TOKEN)).collect(Collectors.toList());
        if (!offeringUuids.isEmpty()) {
            return VirtualRouterOfferingInventory.valueOf1(Q.New(VirtualRouterOfferingVO.class)
                    .in(VirtualRouterOfferingVO_.uuid, offeringUuids)
                    .eq(VirtualRouterOfferingVO_.state, InstanceOfferingState.Enabled)
                    .list());
        }

        sql ="select offering from VirtualRouterOfferingVO offering where offering.zoneUuid = :zoneUuid and offering.state = :state";
        q = dbf.getEntityManager().createQuery(sql, VirtualRouterOfferingVO.class);
        q.setParameter("zoneUuid", guestL3.getZoneUuid());
        q.setParameter("state", InstanceOfferingState.Enabled);
        vos = q.getResultList();
        return vos.isEmpty() ? null : VirtualRouterOfferingInventory.valueOf1(vos);
    }

    @Override
    public List<Flow> getPostCreateFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postCreateFlowsBuilder.getFlows());
        flows.addAll(postCreateFlowExtensionPoints.stream().map(VirtualRouterPostCreateFlowExtensionPoint::virtualRouterPostCreateFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public List<Flow> getPostStartFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postStartFlowsBuilder.getFlows());
        flows.addAll(postStartFlowExtensionPoints.stream().map(VirtualRouterPostStartFlowExtensionPoint::virtualRouterPostStartFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public List<Flow> getPostRebootFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postRebootFlowsBuilder.getFlows());
        flows.addAll(postRebootFlowExtensionPoints.stream().map(VirtualRouterPostRebootFlowExtensionPoint::virtualRouterPostRebootFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public List<Flow> getPostStopFlows() {
        return null;
    }

    @Override
    public List<Flow> getPostMigrateFlows() {
        return null;
    }

    @Override
    public List<Flow> getPostDestroyFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postDestroyFlowsBuilder.getFlows());
        flows.addAll(postDestroyFlowExtensionPoints.stream().map(VirtualRouterPostDestroyFlowExtensionPoint::virtualRouterPostDestroyFlow).collect(Collectors.toList()));
        return flows;
    }

    @Override
    public FlowChain getReconnectFlowChain() {
        FlowChain chain = reconnectFlowsBuilder.build();
        for (VirtualRouterPostReconnectFlowExtensionPoint ext : postReconnectFlowExtensionPoints) {
            chain.then(ext.virtualRouterPostReconnectFlow());
        }
        return chain;
    }

    @Override
    public FlowChain getProvisionConfigChain() {
        FlowChain chain = provisionConfigFlowsBuilder.build();
        for (VirtualProvisionConfigFlowExtensionPoint ext : provisionConfigFlowExtensionPoints) {
            chain.then(ext.provisionConfigFlow());
        }
        return chain;
    }

    @Override
    public int getParallelismDegree(String vrUuid) {
        Integer degree = vrParallelismDegrees.get(vrUuid);
        return degree == null ? VirtualRouterGlobalConfig.COMMANDS_PARALELLISM_DEGREE.value(Integer.class) : degree;
    }

    public void setVirtualRouterPostStartFlows(List<String> virtualRouterPostStartFlows) {
        this.virtualRouterPostStartFlows = virtualRouterPostStartFlows;
    }

    public void setVirtualRouterPostRebootFlows(List<String> virtualRouterPostRebootFlows) {
        this.virtualRouterPostRebootFlows = virtualRouterPostRebootFlows;
    }


    public void setVirtualRouterPostDestroyFlows(List<String> virtualRouterPostDestroyFlows) {
        this.virtualRouterPostDestroyFlows = virtualRouterPostDestroyFlows;
    }

    public void setVirtualRouterPostCreateFlows(List<String> virtualRouterPostCreateFlows) {
        this.virtualRouterPostCreateFlows = virtualRouterPostCreateFlows;
    }

    public void setVirtualRouterReconnectFlows(List<String> virtualRouterReconnectFlows) {
        this.virtualRouterReconnectFlows = virtualRouterReconnectFlows;
    }

    public void setVirtualRouterProvisionConfigFlows(List<String> virtualRouterProvisionConfigFlows) {
        this.virtualRouterProvisionConfigFlows = virtualRouterProvisionConfigFlows;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> classes = new ArrayList<Class>();
        classes.add(APIAttachNetworkServiceToL3NetworkMsg.class);
        classes.add(APIAddIpv6RangeMsg.class);
        classes.add(APIAddIpv6RangeByNetworkCidrMsg.class);
        classes.add(APIAddImageMsg.class);
        return classes;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            validate((APIAttachNetworkServiceToL3NetworkMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeMsg){
            validate((APIAddIpv6RangeMsg) msg);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            validate((APIAddIpv6RangeByNetworkCidrMsg) msg);
        } else if (msg instanceof APIAddImageMsg) {
            validate((APIAddImageMsg) msg);
        }
        return msg;
    }

    void validateIpv6Range(String l3NetworkUuid) {
        if (Q.New(VirtualRouterOfferingVO.class).eq(VirtualRouterOfferingVO_.managementNetworkUuid, l3NetworkUuid).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot add ip range, because l3 network[uuid:%s] is " +
                    "management network of virtual router offering",l3NetworkUuid));
        }

        if (Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3NetworkUuid).in(VmNicVO_.metaData, VirtualRouterNicMetaData.MANAGEMENT_NIC_MASK_STRING_LIST).isExists()) {
            throw new ApiMessageInterceptionException(argerr("cannot add ip range, because l3 network[uuid:%s] is " +
                    "management network of virtual router", l3NetworkUuid));
        }
    }

    private void validate(APIAddIpv6RangeMsg msg) {
        validateIpv6Range(msg.getL3NetworkUuid());
    }

    private void validate(APIAddIpv6RangeByNetworkCidrMsg msg) {
        validateIpv6Range(msg.getL3NetworkUuid());
    }

    private void validate(APIAddImageMsg msg) {
        if (msg.getSystemTags() == null) {
            return;
        }

        for (String tag : msg.getSystemTags()) {
            if (!ImageSystemTags.APPLIANCE_IMAGE_TYPE.isMatch(tag)) {
                continue;
            }

            String type = ImageSystemTags.APPLIANCE_IMAGE_TYPE.getTokenByTag(tag, ImageSystemTags.APPLIANCE_IMAGE_TYPE_TOKEN);
            try {
                ApplianceVmType.valueOf(type);
            } catch (Exception e) {
                throw new ApiMessageInterceptionException(argerr("couldn't add image, because systemTag [%s] " +
                        "includes invalid appliance image type [%s]", tag, type));
            }
        }
    }

    private void validate(APIAttachNetworkServiceToL3NetworkMsg msg) {
        List<String> services = msg.getNetworkServices().get(virtualRouterProvider.getUuid());
        if (services == null) {
            return;
        }

        boolean snat = false;
        boolean portForwarding = false;
        boolean eip = false;

        List<NetworkServiceL3NetworkRefVO> refs = Q.New(NetworkServiceL3NetworkRefVO.class)
                .eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .list();
        for (NetworkServiceL3NetworkRefVO ref : refs) {
            if (ref.getNetworkServiceType().equals(NetworkServiceType.SNAT.toString())) {
                snat = true;
            }
        }

        for (String s : services) {
            if (NetworkServiceType.PortForwarding.toString().equals(s)) {
                portForwarding = true;
            }
            if (NetworkServiceType.SNAT.toString().equals(s)) {
                snat = true;
            }
            if (EipConstant.EIP_NETWORK_SERVICE_TYPE.equals(s)) {
                eip = true;
            }
        }

        if (!snat && eip) {
            throw new ApiMessageInterceptionException(argerr("failed tot attach virtual router network services to l3Network[uuid:%s]. When eip is selected, snat must be selected too", msg.getL3NetworkUuid()));
        }

        if (!snat && portForwarding) {
            throw new ApiMessageInterceptionException(argerr("failed tot attach virtual router network services to l3Network[uuid:%s]. When port forwarding is selected, snat must be selected too", msg.getL3NetworkUuid()));
        }
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<ExpandedQueryStruct>();

        ExpandedQueryStruct struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterEipRef");
        struct.setExpandedInventoryKey("virtualRouterVmUuid");
        struct.setHidden(true);
        struct.setForeignKey("uuid");
        struct.setInventoryClass(VirtualRouterEipRefInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterVipRef");
        struct.setExpandedInventoryKey("virtualRouterVmUuid");
        struct.setHidden(true);
        struct.setForeignKey("uuid");
        struct.setInventoryClass(VirtualRouterVipInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterPortforwardingRef");
        struct.setExpandedInventoryKey("virtualRouterVmUuid");
        struct.setHidden(true);
        struct.setForeignKey("uuid");
        struct.setInventoryClass(VirtualRouterPortForwardingRuleRefInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setExpandedField("virtualRouterOffering");
        struct.setExpandedInventoryKey("uuid");
        struct.setForeignKey("instanceOfferingUuid");
        struct.setInventoryClass(VirtualRouterOfferingInventory.class);
        struct.setInventoryClassToExpand(ApplianceVmInventory.class);
        struct.setSuppressedInventoryClass(InstanceOfferingInventory.class);
        structs.add(struct);

        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        List<ExpandedQueryAliasStruct> aliases = new ArrayList<ExpandedQueryAliasStruct>();

        ExpandedQueryAliasStruct as = new ExpandedQueryAliasStruct();
        as.setInventoryClass(ApplianceVmInventory.class);
        as.setAlias("eip");
        as.setExpandedField("virtualRouterEipRef.eip");
        aliases.add(as);

        as = new ExpandedQueryAliasStruct();
        as.setInventoryClass(ApplianceVmInventory.class);
        as.setAlias("vip");
        as.setExpandedField("virtualRouterVipRef.vip");
        aliases.add(as);

        as = new ExpandedQueryAliasStruct();
        as.setInventoryClass(ApplianceVmInventory.class);
        as.setAlias("portForwarding");
        as.setExpandedField("virtualRouterPortforwardingRef.portForwarding");
        aliases.add(as);
        return aliases;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VmNicInventory> filterVmNicsForEipInVirtualRouter(VipInventory vip, List<VmNicInventory> candidates) {
        if (candidates.isEmpty()){
            return candidates;
        }

        // Note(WeiW) Check vip has attached virtual router network
        Boolean vipForVirtualRouter = null;
        if (vip.getPeerL3NetworkUuids() != null && !vip.getPeerL3NetworkUuids().isEmpty()) {
            // Note(WeiW): The peer l3 must be all of vrouter l3 or flat l3
            NetworkServiceProviderType providerType = nwServiceMgr.
                    getTypeOfNetworkServiceProviderForService(vip.getPeerL3NetworkUuids().get(0), EipConstant.EIP_TYPE);
            // Todo(WeiW): Need to refactor to avoid hard code
            vipForVirtualRouter = providerType.toString().equals(VYOS_ROUTER_PROVIDER_TYPE) ||
                    providerType.toString().equals(VIRTUAL_ROUTER_PROVIDER_TYPE);
        }

        // 1.get the vm nics which are managed by vrouter or virtual router.
        // it also means to ignore vm in flat.
        List<String> privateL3Uuids = candidates.stream().map(VmNicInventory::getL3NetworkUuid).distinct()
                .collect(Collectors.toList());
        /*innerL3: vRouter:Eip network service*/
        List<String> innerl3Uuids = SQL.New("select distinct ref.l3NetworkUuid" +
                " from NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO pro" +
                " where pro.type in (:providerTypes)" +
                " and ref.networkServiceProviderUuid = pro.uuid" +
                " and ref.networkServiceType = :serviceType" +
                " and ref.l3NetworkUuid in (:l3Uuids)", String.class)
                .param("l3Uuids", privateL3Uuids)
                .param("serviceType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                .param("providerTypes", Arrays.asList(
                        VyosConstants.PROVIDER_TYPE.toString(),
                        VirtualRouterConstant.PROVIDER_TYPE.toString()))
                .list();

        List<VmNicInventory> vmNicInVirtualRouter = candidates.stream().filter(nic ->
                innerl3Uuids.contains(nic.getL3NetworkUuid()))
                .collect(Collectors.toList());

        if (vipForVirtualRouter != null && vipForVirtualRouter) {
            List<String> vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), vip.getUuid());
            String vrUuid;
            if (vrUuids == null || vrUuids.isEmpty()) {
                vrUuid = getVipPeerL3NetworkAttachedVirtualRouter(vip);
            } else {
                vrUuid = vrUuids.get(0);
            }
            if (vrUuid != null) {
                List<String> vrAttachedGuestL3 = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.vmInstanceUuid, vrUuid).eq(VmNicVO_.metaData, GUEST_NIC_MASK).listValues();
                logger.debug(String.format("there is virtual router[uuid:%s] associate with vip[uuid:%s], will return candidates from vr guest l3 networks[%s]",
                        vrUuid, vip.getUuid(), vrAttachedGuestL3));
                return candidates.stream()
                        .filter(nic -> vrAttachedGuestL3.contains(nic.getL3NetworkUuid()))
                        .distinct()
                        .collect(Collectors.toList());
            }

            logger.debug(String.format("there are no virtual router associate with vip[uuid:%s], and peer l3 exists, will return candidates from peer l3 networks[%s]",
                    vip.getUuid(), vip.getPeerL3NetworkUuids()));

            return candidates.stream()
                    .filter(nic -> vip.getPeerL3NetworkUuids().contains(nic.getL3NetworkUuid()))
                    .distinct()
                    .collect(Collectors.toList());
        } else if (vipForVirtualRouter != null && !vipForVirtualRouter) {
            logger.debug(String.format("remove all vmnics in virtual router network since vip[uuid:%s] has used in network which is not %s or %s",
                    vip.getUuid(), VYOS_ROUTER_PROVIDER_TYPE, VIRTUAL_ROUTER_PROVIDER_TYPE));
            candidates.removeAll(vmNicInVirtualRouter);
            return candidates;
        }

        // 2. keep vmnics which associated vrouter attached public network of vip
        if (!innerl3Uuids.isEmpty()) {
            List<String> peerL3Uuids = SQL.New("select l3.uuid" +
                    " from VmNicVO nic, L3NetworkVO l3" +
                    " where nic.vmInstanceUuid in " +
                    " (" +
                    " select vm.uuid" +
                    " from VmNicVO nic, ApplianceVmVO vm" +
                    " where nic.l3NetworkUuid = :l3NetworkUuid" +
                    " and nic.vmInstanceUuid = vm.uuid" +
                    " )" +
                    " and l3.uuid = nic.l3NetworkUuid" +
                    " and l3.uuid in (:virtualNetworks)" +
                    " and l3.system = :isSystem")
                                          .param("l3NetworkUuid", vip.getL3NetworkUuid())
                                          .param("virtualNetworks", innerl3Uuids)
                                          .param("isSystem", false)
                                          .list();

            Set<VmNicInventory> r = candidates.stream()
                                              .filter(nic -> peerL3Uuids.contains(nic.getL3NetworkUuid()))
                                              .collect(Collectors.toSet());
            candidates.removeAll(vmNicInVirtualRouter);
            candidates.addAll(r);
        } else {
            candidates.removeAll(vmNicInVirtualRouter);
        }
        return candidates;
    }

    private String getVipPeerL3NetworkAttachedVirtualRouter(VipInventory vip) {
        String vrUuid = null;
        for (String l3Uuid : vip.getPeerL3NetworkUuids()) {
            List<String> vrUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid).eq(VmNicVO_.l3NetworkUuid, l3Uuid).eq(VmNicVO_.metaData, GUEST_NIC_MASK).listValues();
            if (vrUuids == null || vrUuids.isEmpty()) {
                return null;
            }

            vrUuids = Q.New(ApplianceVmVO.class).select(ApplianceVmVO_.uuid).in(ApplianceVmVO_.uuid, vrUuids)
                    .notEq(ApplianceVmVO_.haStatus, ApplianceVmHaStatus.Backup).listValues();
            if (vrUuids == null || vrUuids.isEmpty()) {
                return null;
            }

            vrUuid = vrUuids.get(0);
        }

        return vrUuid;
    }

    private String getDedicatedRoleVrUuidFromVrUuids(List<String> uuids, String loadBalancerUuid) {
        Set<String> vrUuids = new HashSet<>(uuids);

        if (vrUuids.size() == 2
                && LoadBalancerSystemTags.SEPARATE_VR.hasTag(loadBalancerUuid)
                && vrUuids.stream().anyMatch(uuid -> VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(uuid))) {
            for (String uuid : vrUuids) {
                if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(uuid)) {
                    return uuid;
                }
            }
        }

        if (vrUuids.size() == 1) {
            return vrUuids.iterator().next();
        } else if (vrUuids.size() == 0) {
            return null;
        } else {
            throw new CloudRuntimeException(String.format("there are multiple virtual routers[uuids:%s]", vrUuids));
        }
    }

    private String getVirtualRouterVmAttachedLoadBalancer(String lbUuid) {
        List<String> vrUuids = lbProxy.getVrUuidsByNetworkService(LoadBalancerVO.class.getSimpleName(), lbUuid);
        String vrUuid = getDedicatedRoleVrUuidFromVrUuids(vrUuids, lbUuid);

        if (vrUuid != null) {
            return vrUuid;
        }

        final List<String> peerL3NetworkUuids = SQL.New("select peer.l3NetworkUuid " +
                "from LoadBalancerVO lb, VipVO vip, VipPeerL3NetworkRefVO peer " +
                "where lb.vipUuid = vip.uuid " +
                "and vip.uuid = peer.vipUuid " +
                "and lb.uuid = :lbUuid").param("lbUuid", lbUuid).list();

        if (peerL3NetworkUuids != null && !peerL3NetworkUuids.isEmpty()) {
            vrUuids = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                       .in(VmNicVO_.l3NetworkUuid, peerL3NetworkUuids)
                       .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                       .listValues();
            if (vrUuids != null && !vrUuids.isEmpty()) {
                /* filter backup router */
                vrUuids = Q.New(ApplianceVmVO.class).select(ApplianceVmVO_.uuid).in(ApplianceVmVO_.uuid, vrUuids)
                           .notEq(ApplianceVmVO_.haStatus, ApplianceVmHaStatus.Backup).listValues();
            }

            vrUuid = getDedicatedRoleVrUuidFromVrUuids(vrUuids, lbUuid);

            if (vrUuid != null) {
                return vrUuid;
            }
        }

        VipVO lbVipVO = SQL.New("select vip from LoadBalancerVO lb, VipVO vip " +
                "where lb.vipUuid = vip.uuid " +
                "and lb.uuid = :lbUuid")
                           .param("lbUuid", lbUuid).find();

        if (lbVipVO != null) {
            List<String> useFor = Q.New(VipNetworkServicesRefVO.class).select(VipNetworkServicesRefVO_.serviceType).eq(VipNetworkServicesRefVO_.vipUuid, lbVipVO.getUuid()).listValues();
            if(useFor != null && useFor.contains(SNAT_NETWORK_SERVICE_TYPE)) {
                vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), lbVipVO.getUuid());
                return vrUuids.get(0);
            }
        }
        return null;
    }

    @Override
    public List<L3NetworkInventory> getPeerL3NetworksForLoadBalancer(String lbUuid, List<L3NetworkInventory> candidates) {
        if(candidates == null || candidates.isEmpty()){
            return candidates;
        }

        /*get vr*/
        String vrUuid = getVirtualRouterVmAttachedLoadBalancer(lbUuid);

        if (vrUuid != null) {
            return new SQLBatchWithReturn<List<L3NetworkInventory>>(){
                @Override
                protected List<L3NetworkInventory> scripts() {
                    List<String> guestL3Uuids = Q.New(VmNicVO.class)
                                                 .select(VmNicVO_.l3NetworkUuid)
                                                 .eq(VmNicVO_.vmInstanceUuid, vrUuid)
                                                 .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                                                 .listValues();

                    if (guestL3Uuids == null || guestL3Uuids.isEmpty()) {
                        return new ArrayList<>();
                    }
                    return candidates.stream().filter(l3 -> guestL3Uuids.contains(l3.getUuid())).collect(Collectors.toList());
                }
            }.execute();
        }

        /*get peer network of vip, vr has been deleted, so just return these peer networks*/
        final List<String> peerL3NetworkUuids = SQL.New("select peer.l3NetworkUuid " +
                "from LoadBalancerVO lb, VipVO vip, VipPeerL3NetworkRefVO peer " +
                "where lb.vipUuid = vip.uuid " +
                "and vip.uuid = peer.vipUuid " +
                "and lb.uuid = :lbUuid").param("lbUuid", lbUuid).list();

        if (peerL3NetworkUuids != null && !peerL3NetworkUuids.isEmpty()) {
            return candidates.stream().filter(n -> peerL3NetworkUuids.contains(n.getUuid()))
                             .collect(Collectors.toList());
        }

        return new SQLBatchWithReturn<List<L3NetworkInventory>>(){

            @Override
            protected List<L3NetworkInventory> scripts() {

                //1.get the l3 which are managed by vrouter or virtual router.
                List<String>  inners = sql("select distinct l3.uuid from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO pro" +
                        " where l3.uuid = ref.l3NetworkUuid and ref.networkServiceProviderUuid = pro.uuid and l3.uuid in (:l3Uuids)" +
                        " and pro.type in (:providerType)", String.class)
                        .param("l3Uuids", candidates.stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList()))
                        .param("providerType", Arrays.asList(VyosConstants.PROVIDER_TYPE.toString(),VirtualRouterConstant.PROVIDER_TYPE.toString()))
                        .list();

                List<L3NetworkInventory> ret = candidates.stream().filter(l3 -> inners.contains(l3.getUuid())).collect(Collectors.toList());
                if(ret.size() == 0){
                    return new ArrayList<>();
                }

                VipVO lbVipVO = SQL.New("select vip from LoadBalancerVO lb, VipVO vip " +
                        "where lb.vipUuid = vip.uuid " +
                        "and lb.uuid = :lbUuid")
                                   .param("lbUuid", lbUuid).find();

                //2.check the l3 is peer l3 of the loadbalancer
                L3NetworkVO vipNetwork = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, lbVipVO.getL3NetworkUuid()).find();
                List<String> peerL3Uuids = SQL.New("select l3.uuid" +
                        " from VmNicVO nic, L3NetworkVO l3"  +
                        " where nic.vmInstanceUuid in " +
                        " (" +
                        " select vm.uuid" +
                        " from VmNicVO nic, ApplianceVmVO vm" +
                        " where nic.l3NetworkUuid = :l3NetworkUuid" +
                        " and nic.vmInstanceUuid = vm.uuid" +
                        " )"+
                        " and l3.uuid = nic.l3NetworkUuid" +
                        " and nic.metaData = :metaData" +
                        " and l3.system = :isSystem")
                                              .param("l3NetworkUuid", vipNetwork.getUuid())
                                              .param("isSystem", false)
                                              .param("metaData", VirtualRouterNicMetaData.GUEST_NIC_MASK.toString())
                                              .list();

                // 3. filter all the l3 networks which are not managed by vrouter or virtual router currently and
                // have been attached the virtualrouter offers,
                // such as without other services except for vrouter lb services

                List<String> excludeL3Uuids = SQL.New("select l3.uuid" +
                        " from VmNicVO nic, L3NetworkVO l3"  +
                        " where l3.uuid in (:l3NetworkUuids)" +
                        " and l3.uuid = nic.l3NetworkUuid" +
                        " and nic.metaData = :metaData" +
                        " and l3.system = :isSystem")
                                                 .param("l3NetworkUuids", inners)
                                                 .param("isSystem", false)
                                                 .param("metaData", VirtualRouterNicMetaData.GUEST_NIC_MASK.toString())
                                                 .list();
                inners.removeAll(excludeL3Uuids);
                peerL3Uuids.addAll(selectGuestL3NetworksNeedingSpecificNetworkService(inners,
                        LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE, vipNetwork.getUuid()));

                return ret.stream().filter(l3 -> peerL3Uuids.contains(l3.getUuid())).collect(Collectors.toList());
            }
        }.execute();
    }

    @Override
    public List<VmNicInventory> getCandidateVmNicsForLoadBalancerInVirtualRouter(APIGetCandidateVmNicsForLoadBalancerMsg msg, List<VmNicInventory> candidates) {
        if(candidates == null || candidates.isEmpty()){
            return candidates;
        }

        String vrUuid = getVirtualRouterVmAttachedLoadBalancer( msg.getLoadBalancerUuid());

        if (vrUuid != null) {
            return getCandidateVmNicsIfLoadBalancerBound(msg, candidates, vrUuid);
        }

        final List<String> peerL3NetworkUuids = SQL.New("select peer.l3NetworkUuid " +
                "from LoadBalancerVO lb, VipVO vip, VipPeerL3NetworkRefVO peer " +
                "where lb.vipUuid = vip.uuid " +
                "and vip.uuid = peer.vipUuid " +
                "and lb.uuid = :lbUuid")
                .param("lbUuid", msg.getLoadBalancerUuid())
                .list();

        if (peerL3NetworkUuids != null && !peerL3NetworkUuids.isEmpty()) {
            return getCandidateVmNicsIfPeerL3NetworkExists(msg, candidates, peerL3NetworkUuids);
        }

        return new SQLBatchWithReturn<List<VmNicInventory>>(){

            @Override
            protected List<VmNicInventory> scripts() {
                VipVO lbVipVO = SQL.New("select vip from LoadBalancerVO lb, VipVO vip " +
                        "where lb.vipUuid = vip.uuid " +
                        "and lb.uuid = :lbUuid")
                                   .param("lbUuid", msg.getLoadBalancerUuid()).find();

                //1.get the l3 networks which has the vrouter lb network service.
                List<String>  inners = sql("select l3.uuid from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO pro" +
                        " where l3.uuid = ref.l3NetworkUuid and ref.networkServiceProviderUuid = pro.uuid and l3.uuid in (:l3Uuids)" +
                        " and pro.type in (:providerType)", String.class)
                        .param("l3Uuids", candidates.stream().map(VmNicInventory::getL3NetworkUuid).collect(Collectors.toList()))
                        .param("providerType", Arrays.asList(VyosConstants.PROVIDER_TYPE.toString(),VirtualRouterConstant.PROVIDER_TYPE.toString()))
                        .list();

                List<VmNicInventory> ret = candidates.stream().filter(nic -> inners.contains(nic.getL3NetworkUuid())).collect(Collectors.toList());
                if(ret.size() == 0){
                    return new ArrayList<VmNicInventory>();
                }

                //2.check the l3 of vm nic is peer l3 of the loadbalancer
                L3NetworkVO vipNetwork = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, lbVipVO.getL3NetworkUuid()).find();
                List<String> peerL3Uuids = SQL.New("select l3.uuid" +
                        " from VmNicVO nic, L3NetworkVO l3"  +
                        " where nic.vmInstanceUuid in " +
                        " (" +
                        " select vm.uuid" +
                        " from VmNicVO nic, ApplianceVmVO vm" +
                        " where nic.l3NetworkUuid = :l3NetworkUuid" +
                        " and nic.vmInstanceUuid = vm.uuid" +
                        " )"+
                        " and l3.uuid = nic.l3NetworkUuid" +
                        " and nic.metaData = :metaData" +
                        " and l3.system = :isSystem")
                                              .param("l3NetworkUuid", vipNetwork.getUuid())
                                              .param("isSystem", false)
                                              .param("metaData", VirtualRouterNicMetaData.GUEST_NIC_MASK.toString())
                                              .list();
                // 3. filter all the l3 networks which are not managed by vrouter or virtual router currently and
                // have been attached the virtualrouter offers,
                // such as without other services except for vrouter lb services

                List<String> excludeL3Uuids = SQL.New("select l3.uuid" +
                        " from VmNicVO nic, L3NetworkVO l3"  +
                        " where l3.uuid in (:l3NetworkUuids)" +
                        " and l3.uuid = nic.l3NetworkUuid" +
                        " and nic.metaData = :metaData" +
                        " and l3.system = :isSystem")
                                              .param("l3NetworkUuids", inners)
                                              .param("isSystem", false)
                                              .param("metaData", VirtualRouterNicMetaData.GUEST_NIC_MASK.toString())
                                              .list();
                inners.removeAll(excludeL3Uuids);
                peerL3Uuids.addAll(selectGuestL3NetworksNeedingSpecificNetworkService(inners,
                        LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE, vipNetwork.getUuid()));
                return ret.stream().filter(nic -> peerL3Uuids.contains(nic.getL3NetworkUuid())).collect(Collectors.toList());

            }
        }.execute();
    }

    private List<VmNicInventory> getCandidateVmNicsIfPeerL3NetworkExists(APIGetCandidateVmNicsForLoadBalancerMsg msg, List<VmNicInventory> candidates, List<String> peerL3NetworkUuids) {
        LoadBalancerListenerVO listenerVO = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
        final List<String> attachedVmNicUuids = listenerVO.getAttachedVmNics();
        return candidates.stream()
                .filter(n -> peerL3NetworkUuids.contains(n.getL3NetworkUuid()))
                .filter(n -> !attachedVmNicUuids.contains(n.getUuid()))
                .collect(Collectors.toList());
    }

    private List<VmNicInventory> getCandidateVmNicsIfLoadBalancerBound(APIGetCandidateVmNicsForLoadBalancerMsg msg, List<VmNicInventory> candidates, String vrUuid) {
        List<String> candidatesUuids = candidates.stream().map(VmNicInventory::getUuid).collect(Collectors.toList());
        logger.debug(String.format("loadbalancer[uuid:%s] has bound to virtual router[uuid:%s], " +
                        "continue working with vmnics:%s", msg.getLoadBalancerUuid(), vrUuid, candidatesUuids));

        return new SQLBatchWithReturn<List<VmNicInventory>>(){

            @Override
            protected List<VmNicInventory> scripts() {
                List<String> guestL3Uuids = Q.New(VmNicVO.class)
                        .select(VmNicVO_.l3NetworkUuid)
                        .eq(VmNicVO_.vmInstanceUuid, vrUuid)
                        .eq(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK)
                        .listValues();

                if (guestL3Uuids == null || guestL3Uuids.isEmpty()) {
                    return new ArrayList<>();
                }

                List<String> vmNicUuids = SQL.New("select nic.uuid from VmNicVO nic, VmInstanceEO vm " +
                        "where vm.uuid = nic.vmInstanceUuid " +
                        "and vm.type = :vmType " +
                        "and vm.state in (:vmState) " +
                        "and nic.l3NetworkUuid in (:l3s) " +
                        "and nic.metaData is NULL")
                        .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                        .param("vmState", asList(VmInstanceState.Running, VmInstanceState.Stopped))
                        .param("l3s", guestL3Uuids)
                        .list();

                LoadBalancerListenerVO listenerVO = dbf.findByUuid(msg.getListenerUuid(), LoadBalancerListenerVO.class);
                final List<String> finalAttachedVmNicUuids = listenerVO.getAttachedVmNics();

                return candidates.stream()
                        .filter( nic -> vmNicUuids.contains(nic.getUuid()))
                        .filter( nic -> !finalAttachedVmNicUuids.contains(nic.getUuid()))
                        .collect(Collectors.toList());
            }
        }.execute();
    }

    private List<ApplianceVmVO> applianceVmsToBeDeleted(List<ApplianceVmVO> applianceVmVOS, List<String> deletedUuids) {
        List<ApplianceVmVO> vos = new ArrayList<>();
        for (ApplianceVmVO vo : applianceVmVOS) {
            VirtualRouterVmVO vo_dbf = dbf.findByUuid(vo.getUuid(), VirtualRouterVmVO.class);
            if(vo_dbf == null) {
                continue;
            }
            VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vo_dbf);

            List<String> l3Uuids = new ArrayList<>(vrInv.getGuestL3Networks());
            l3Uuids.add(vrInv.getPublicNetworkUuid());
            l3Uuids.add(vrInv.getManagementNetworkUuid());
            l3Uuids.add(vrInv.getDefaultRouteL3NetworkUuid());
            for(String uuid: l3Uuids) {
                if (deletedUuids.contains(uuid)) {
                    vos.add(vo);
                    break;
                }
            }
        }

        return vos;
    }

    List<VmNicInventory> applianceVmsAdditionalPublicNic(List<ApplianceVmVO> applianceVmVOS, List<String> parentIssuerUuids) {
        List<VmNicInventory> toDeleteNics = new ArrayList<>();
        for (ApplianceVmVO vo : applianceVmVOS) {
            VirtualRouterVmVO vr_dbf = dbf.findByUuid(vo.getUuid(), VirtualRouterVmVO.class);
            if(vr_dbf == null) {
                continue;
            }
            VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vr_dbf);
            for (VmNicInventory nic : vrInv.getAdditionalPublicNics()) {
                /* skip default router nic */
                if (nic.getL3NetworkUuid().equals(vrInv.getDefaultRouteL3NetworkUuid())) {
                    continue;
                }

                if (parentIssuerUuids.contains(nic.getL3NetworkUuid())) {
                    toDeleteNics.add(nic);
                }
            }
        }

        return toDeleteNics;
    }

    void applianceVmsDeleteIpByIpRanges(List<ApplianceVmVO> applianceVmVOS,
                                                    List<String> ipv4RangeUuids, List<String> ipv6RangeUuids) {
        FutureCompletion completion = new FutureCompletion(null);

        List<ReturnIpMsg> toDeleteIps = new ArrayList<>();
        for (ApplianceVmVO vo : applianceVmVOS) {
            /* because nic maybe be deleted, so refresh the appliance */
            vo = dbf.findByUuid(vo.getUuid(), ApplianceVmVO.class);
            for (VmNicVO nic : vo.getVmNics()) {
                for (UsedIpVO ip : nic.getUsedIps()) {
                    if (ip.getIpVersion() == IPv6Constants.IPv4 && ipv4RangeUuids.contains(ip.getIpRangeUuid())) {
                        ReturnIpMsg rmsg = new ReturnIpMsg();
                        rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                        rmsg.setUsedIpUuid(ip.getUuid());
                        rmsg.setNicUuid(nic.getUuid());
                        toDeleteIps.add(rmsg);
                        break;
                    }
                    if (ip.getIpVersion() == IPv6Constants.IPv6 && ipv6RangeUuids.contains(ip.getIpRangeUuid())) {
                        ReturnIpMsg rmsg = new ReturnIpMsg();
                        rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                        rmsg.setUsedIpUuid(ip.getUuid());
                        rmsg.setNicUuid(nic.getUuid());
                        toDeleteIps.add(rmsg);
                        break;
                    }
                }
            }
        }

        if (toDeleteIps.isEmpty()) {
            return;
        }
        new While<>(toDeleteIps).step((rmsg, wcomp) -> {
            bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, rmsg.getL3NetworkUuid());
            bus.send(rmsg, new CloudBusCallBack(rmsg) {
                @Override
                public void run(MessageReply reply) {
                    for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                        ext.afterDelIpAddress(rmsg.getNicUuid(), rmsg.getUsedIpUuid());
                    }
                    wcomp.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    List<VmNicVO> applianceVmsToDeleteNicByIpRanges(List<ApplianceVmVO> applianceVmVOS, List<String> iprUuids) {
        List<VmNicVO> toDeleteNics = new ArrayList<>();
        for (ApplianceVmVO vo : applianceVmVOS) {
            for (VmNicVO nic : vo.getVmNics()) {
                for (UsedIpVO ip : nic.getUsedIps()) {
                    if (!iprUuids.contains(ip.getIpRangeUuid())) {
                        continue;
                    }

                    if (!VirtualRouterNicMetaData.isGuestNic(nic)) {
                        toDeleteNics.add(nic);
                        continue;
                    }

                    /* for guest nic, if there has other ip range left, will not delete the nic */
                    if (Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ip.getL3NetworkUuid())
                            .eq(NormalIpRangeVO_.ipVersion, ip.getIpVersion()).count() > 1) {
                        continue;
                    }
                    toDeleteNics.add(nic);
                }
            }
        }

        return toDeleteNics;
    }

    private List<ApplianceVmVO> applianceVmsToBeDeletedByIpRanges(List<ApplianceVmVO> applianceVmVOS, List<String> iprUuids) {
        List<ApplianceVmVO> toDeleted = new ArrayList<>();
        List<String> l3Uuids = Q.New(IpRangeVO.class).in(IpRangeVO_.uuid, iprUuids).select(IpRangeVO_.l3NetworkUuid).listValues();
        for (ApplianceVmVO vos : applianceVmVOS) {
            for (VmNicVO nic : vos.getVmNics()) {
                /* for additional public network, only delete nic */
                if (VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
                    continue;
                }

                /* if any ip of the nic is deleted, delete the appliance vm */
                if (nic.getUsedIps().stream().anyMatch(ip -> iprUuids.contains(ip.getIpRangeUuid()))) {
                    toDeleted.add(vos);
                    break;
                }

                /* for virtual router, if all ip ranges of guest nic delete, will delete the virtual router,
                   but gateway ip is no allocated in UsedIpVO: there is no ipv6 for virtual router */
                if (VirtualRouterNicMetaData.isGuestNic(nic) && nic.getUsedIpUuid() == null && l3Uuids.contains(nic.getL3NetworkUuid())) {
                    if (Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                            .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).count() <= 1) {
                        toDeleted.add(vos);
                    }
                    break;
                }
            }
        }

        return toDeleted;
    }

    @Override
    public List<ApplianceVmVO> filterApplianceVmCascade(List<ApplianceVmVO> applianceVmVOS, CascadeAction action,
                                                        String parentIssuer,
                                                        List<String> parentIssuerUuids,
                                                        List<VmNicInventory> toDeleteNics,
                                                        List<UsedIpInventory> toDeleteIps) {
        logger.debug(String.format("filter appliance vm type with parentIssuer [type: %s, uuids: %s]", parentIssuer, parentIssuerUuids));
        if (parentIssuer.equals(L3NetworkVO.class.getSimpleName())) {
            List<ApplianceVmVO> vos = applianceVmsToBeDeleted(applianceVmVOS, parentIssuerUuids);

            applianceVmVOS.removeAll(vos);
            toDeleteNics.addAll(applianceVmsAdditionalPublicNic(applianceVmVOS, parentIssuerUuids));

            return vos;
        } else if (parentIssuer.equals(IpRangeVO.class.getSimpleName())) {
            List<ApplianceVmVO> vos = applianceVmsToBeDeletedByIpRanges(applianceVmVOS, parentIssuerUuids);
            applianceVmVOS.removeAll(vos);
            toDeleteNics.addAll(VmNicInventory.valueOf(applianceVmsToDeleteNicByIpRanges(applianceVmVOS, parentIssuerUuids)));

            return vos;
        } else {
            return applianceVmVOS;
        }
    }

    private void reconenctVirtualRouter(String vrUUid, boolean statusChange) {
        ReconnectVirtualRouterVmMsg msg = new ReconnectVirtualRouterVmMsg();
        msg.setVirtualRouterVmUuid(vrUUid);
        msg.setStatusChange(statusChange);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrUUid);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("virtual router[uuid:%s] reconnection failed, because %s", vrUUid, reply.getError()));
                } else {
                    logger.debug(String.format("virtual router[uuid:%s] reconnect successfully", vrUUid));
                }
            }
        });
    }

    private void handle(CheckVirtualRouterVmVersionMsg cmsg) {
        CheckVirtualRouterVmVersionReply reply = new CheckVirtualRouterVmVersionReply();

        /* reply message back asap to avoid blocking mn node startup */
        bus.reply(cmsg, reply);

        VirtualRouterVmVO vrVo = dbf.findByUuid(cmsg.getVirtualRouterVmUuid(), VirtualRouterVmVO.class);
        VirtualRouterVmInventory inv = VirtualRouterVmInventory.valueOf(vrVo);
        if (VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE.equals(inv.getApplianceVmType())) {
            return;
        }

        if (vrVo.getStatus() == ApplianceVmStatus.Connecting) {
            reconenctVirtualRouter(inv.getUuid(), false);
            return;
        }

        if (vrVo.getHaStatus() == ApplianceVmHaStatus.Backup) {
            reconenctVirtualRouter(inv.getUuid(), false);
            return;
        }

        vyosVersionManager.vyosRouterVersionCheck(inv.getUuid(), new ReturnValueCompletion<VyosVersionCheckResult>(cmsg) {
            @Override
            public void success(VyosVersionCheckResult returnValue) {
                if (returnValue.isNeedReconnect()) {
                    logger.warn(String.format("virtual router[uuid: %s] need to be reconnected", inv.getUuid()));
                    reconenctVirtualRouter(inv.getUuid(), true);
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
            }
        });
    }

    @Override
    public void managementNodeReady() {
        List<VirtualRouterVmVO> vrVos = Q.New(VirtualRouterVmVO.class).list();
        for (VirtualRouterVmVO vrVo : vrVos) {
            CheckVirtualRouterVmVersionMsg msg = new CheckVirtualRouterVmVersionMsg();
            msg.setVirtualRouterVmUuid(vrVo.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VirtualRouterConstant.SERVICE_ID, vrVo.getUuid());
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("virtual router[uuid:%s] check version message failed, because %s", vrVo.getUuid(), reply.getError()));
                    } else {
                        logger.debug(String.format("virtual router[uuid:%s] check version message successfully", vrVo.getUuid()));
                    }
                }
            });
        }
    }

    @Override
    public void cleanupVip(String uuid) {
        SQL.New(VirtualRouterVipVO.class).eq(VirtualRouterVipVO_.uuid, uuid).delete();
    }

    @Override
    public List<String> getL3NetworkForEipInVirtualRouter(String networkServiceProviderType, VipInventory vip, List<String> l3Uuids) {
        if (networkServiceProviderType.equals(VYOS_ROUTER_PROVIDER_TYPE)) {
            /* get vpc network or vrouter network */
            return SQL.New("select distinct l3.uuid" +
                    " from  L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider" +
                    " where l3.uuid = ref.l3NetworkUuid and ref.networkServiceProviderUuid = provider.uuid" +
                    " and ref.networkServiceType = :serviceType and provider.type = :providerType" +
                    " and l3.ipVersion in (:ipVersions) and l3.uuid in (:l3Uuids)")
                    .param("serviceType", EipConstant.EIP_NETWORK_SERVICE_TYPE)
                    .param("providerType", VYOS_ROUTER_PROVIDER_TYPE)
                    .param("ipVersions", vip.getCandidateIpversion())
                    .param("l3Uuids", l3Uuids)
                    .list();
        }
        return new ArrayList<>();
    }

    public VmNicInventory getSnatPubicInventory(VirtualRouterVmInventory vrInv) {
        return getSnatPubicInventory(vrInv, vrInv.getDefaultRouteL3NetworkUuid());
    }

    public VmNicInventory getSnatPubicInventory(VirtualRouterVmInventory vrInv, String L3NetworkUuid) {
        VmNicInventory publicNic = null;

        for (VmNicInventory vnic : vrInv.getVmNics()) {
            if (vnic.getL3NetworkUuid().equals(L3NetworkUuid)) {
                publicNic = new VmNicInventory(dbf.findByUuid(vnic.getUuid(), VmNicVO.class));
            }
        }

        /* this code is ugly because when mgt network is same to public network,
        * for ha router, the vip is different from nic ip */
        UsedIpInventory ip4 = null, ip6 = null;
        for (UsedIpInventory ip : publicNic.getUsedIps()) {
            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                ip4 = ip;
            } else if (ip.getIpVersion() == IPv6Constants.IPv6) {
                ip6 = ip;
            }
        }
        List<String> publicIps = null;
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            publicIps = ext.getPublicIp(vrInv.getUuid(), L3NetworkUuid);
        }
        if (publicIps != null && !publicIps.isEmpty()) {
            for (String ip : publicIps) {
                if (NetworkUtils.isIpv4Address(ip)) {
                    publicNic.setIp(ip);
                    if (ip4 != null) {
                        ip4.setIp(ip);
                    }
                } else {
                    if (ip6 != null) {
                        ip6.setIp(ip);
                    }
                }
            }
        }

        return publicNic;
    }

    private List<VirtualRouterCommands.SNATInfo> getSnatInfo(VirtualRouterVmInventory vrInv) {
        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrInv.getUuid()).find();
        if (vrVO == null) {
            return null;
        }
        ApplianceVmSubTypeFactory subTypeFactory = apvmFactory.getApplianceVmSubTypeFactory(vrVO.getApplianceVmType());
        ApplianceVm app = subTypeFactory.getSubApplianceVm(vrVO);
        List<String> snatL3Uuids = app.getSnatL3NetworkOnRouter(vrVO.getUuid());
        if (snatL3Uuids.isEmpty()) {
            return null;
        }

        List<String> nwServed = vrInv.getAllL3Networks();
        nwServed = selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.SNAT);
        if (nwServed.isEmpty()) {
            return null;
        }

        VmNicInventory publicNic = getSnatPubicInventory(vrInv);
        if (publicNic.isIpv6OnlyNic()) {
            return null;
        }

        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<>();
        for (VmNicInventory vnic : vrInv.getVmNics()) {
            if (nwServed.contains(vnic.getL3NetworkUuid()) && !vnic.isIpv6OnlyNic()) {
                VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
                info.setPrivateNicIp(vnic.getIp());
                info.setPrivateNicMac(vnic.getMac());
                info.setPublicIp(publicNic.getIp());
                info.setPublicNicMac(publicNic.getMac());
                info.setSnatNetmask(vnic.getNetmask());
                info.setPrivateGatewayIp(vnic.getGateway());
                snatInfo.add(info);
            }
        }

        return snatInfo;
    }


    @Transactional
    protected void changeVirtualRouterNicMetaData(String vrUuid, String newL3Uuid, String oldL3Uuid) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        for (VmNicVO nic : vrVo.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(oldL3Uuid)) {
                VirtualRouterNicMetaData.removePublicToNic(nic);
                VirtualRouterNicMetaData.addAdditionalPublicToNic(nic);
                dbf.update(nic);
            } else if (nic.getL3NetworkUuid().equals(newL3Uuid)) {
                VirtualRouterNicMetaData.removeAdditionalPublicToNic(nic);
                VirtualRouterNicMetaData.addPublicToNic(nic);
                dbf.update(nic);
            }
        }
    }

    public void changeVirutalRouterDefaultL3Network(String vrUuid, String newL3Uuid, String oldL3Uuid, Completion completion) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vrVo);

        VmNicVO newNic = CollectionUtils.findOneOrNull(vrVo.getVmNics(), arg -> arg.getL3NetworkUuid().equals(newL3Uuid));
        DebugUtils.Assert(newNic != null, String.format("cannot find nic for old default network[uuid:%s]", newL3Uuid));

        VirtualRouterCommands.NicInfo newNicInfo  = new VirtualRouterCommands.NicInfo();
        newNicInfo.setMac(newNic.getMac());
        newNicInfo.setState(newNic.getState().toString());
        for (UsedIpVO ip : newNic.getUsedIps()) {
            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                newNicInfo.setGateway(ip.getGateway());
                newNicInfo.setIp(ip.getIp());
            } else {
                newNicInfo.setGateway6(ip.getGateway());
                newNicInfo.setIp6(ip.getIp());
            }
        }

        VirtualRouterCommands.ChangeDefaultNicCmd cmd = new VirtualRouterCommands.ChangeDefaultNicCmd();
        cmd.setNewNic(newNicInfo);

        List<VirtualRouterCommands.SNATInfo> snatInfos = getSnatInfo(vrInv);
        if (snatInfos != null) {
            cmd.setSnats(snatInfos);
        }

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vrUuid);
        msg.setPath(VirtualRouterConstant.VR_CHANGE_DEFAULT_ROUTE_NETWORK);
        msg.setCommand(cmd);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.SetSNATRsp ret = re.toResponse(VirtualRouterCommands.SetSNATRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("update virtual router [uuid:%s] default network failed, because %s",
                            vrUuid, ret.getError());
                    completion.fail(err);
                } else {
                    changeVirtualRouterNicMetaData(vrUuid, newL3Uuid, oldL3Uuid);
                    completion.success();
                }
            }
        });
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct changeDefaultNic = new VirtualRouterHaCallbackStruct();
        changeDefaultNic.type = VirtualRouterConstant.VR_CHANGE_DEFAULT_ROUTE_JOB;
        changeDefaultNic.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                ChangeDefaultRouteTaskData d = JSONObjectUtil.toObject(task.getJsonData(), ChangeDefaultRouteTaskData.class);
                String newL3Uuid = d.getNewL3uuid();
                String oldL3Uuid = d.getOldL3uuid();
                changeVirutalRouterDefaultL3Network(vrUuid, newL3Uuid, oldL3Uuid, completion);
            }
        };
        structs.add(changeDefaultNic);

        return structs;
    }

    @Override
    public void afterAddIpRange(IpRangeInventory ipr, List<String> systemTags) {
        if (ipr.getIpRangeType() != IpRangeType.Normal) {
            return;
        }

        /* when change a IPv4/IPv6 network to dual stack network, after add the ip range,
           allocate the gateway ip to virtual router, but only after reboot virtual router,
            virtual router will be configured with the gateway */
        List<VmNicVO> vnics = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, ipr.getL3NetworkUuid())
                .notNull(VmNicVO_.metaData).list();
        if (vnics.isEmpty()) {
            return;
        }

        Map<String, String> haIpMap = new HashMap<>();
        /* for ha router, same ip maybe allocated twice */
        for (VmNicVO nic : vnics) {
            boolean allocated = false;
            for (UsedIpVO ip : nic.getUsedIps()) {
                if (ip.getIpVersion() == ipr.getIpVersion()) {
                    allocated = true;
                    break;
                }
            }

            if (allocated) {
                continue;
            }

            String haGroupUuid = haBackend.getVirtualRouterHaUuid(nic.getVmInstanceUuid());
            AllocateIpMsg msg = new AllocateIpMsg();
            msg.setL3NetworkUuid(ipr.getL3NetworkUuid());
            if (VirtualRouterNicMetaData.isGuestNic(nic)) {
                msg.setRequiredIp(ipr.getGateway());
            }
            msg.setIpVersion(ipr.getIpVersion());
            if (haGroupUuid != null && haIpMap.get(haGroupUuid) != null) {
                msg.setDuplicatedIpAllowed(true);
                msg.setRequiredIp(haIpMap.get(haGroupUuid));
            }
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ipr.getL3NetworkUuid());
            MessageReply reply = bus.call(msg);
            if (!reply.isSuccess()) {
                throw new FlowException(reply.getError());
            }

            AllocateIpReply areply = (AllocateIpReply) reply;
            if (haGroupUuid != null) {
                haIpMap.put(haGroupUuid, areply.getIpInventory().getIp());
            }
            for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                ext.afterAddIpAddress(nic.getUuid(), areply.getIpInventory().getUuid());
            }
        }
    }

    @Override
    public String filterName() {
        return ImageSystemTags.APPLIANCE_IMAGE_TYPE_TOKEN;
    }

    @Override
    public String convertFilterNameToZQL(String filterName) {
        String[] ss = filterName.split(":");
        try {
            ApplianceVmType.valueOf(ss[1]);
            return String.format("has ('applianceType::%s')", ss[1]);
        } catch (Exception e) {
            throw new OperationFailureException(argerr("invalid ApplianceVmType %s", ss[1]));
        }
    }

    /* filter out the vips that belong to other applianceVm */
    @Override
    public List<String> getVirtualRouterVips(String vrUuid, List<String> vipUuids) {
        List<String> ret = new ArrayList<>();
        String peerUuid = haBackend.getVirtualRouterPeerUuid(vrUuid);

        for (String uuid : vipUuids) {
            List<String> vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), uuid);
            if (vrUuids == null || vrUuids.isEmpty()) {
                ret.add(uuid);
                continue;
            }

            if (vrUuids.contains(vrUuid) || vrUuids.contains(peerUuid)) {
                ret.add(uuid);
            }

            /* this vip is belonged to this vr */
        }

        return ret;
    }

    @Override
    public List<String> getPublicL3UuidsOfPrivateL3(L3NetworkVO privateL3) {
        List<IpRangeVO> ipv4RangeVOS = privateL3.getIpRanges().stream()
                .filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeVO> ipv6RangeVOS = privateL3.getIpRanges().stream()
                .filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());

        if (ipv6RangeVOS.isEmpty() && ipv4RangeVOS.isEmpty()) {
            return new ArrayList<>();
        }

        String vrUuid = null;
        if (ipv4RangeVOS.isEmpty()) {
            vrUuid = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                    .eq(VmNicVO_.l3NetworkUuid, privateL3.getUuid())
                    .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST)
                    .eq(VmNicVO_.ip, ipv6RangeVOS.get(0).getGateway())
                    .limit(1).findValue();
        } else {
            List<String> ipv4RangeUuids = ipv4RangeVOS.stream().map(IpRangeVO::getUuid).collect(Collectors.toList());
            List<String> gateways = Q.New(NormalIpRangeVO.class).select(NormalIpRangeVO_.gateway)
                    .in(NormalIpRangeVO_.uuid, ipv4RangeUuids).listValues();
            vrUuid = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                    .eq(VmNicVO_.l3NetworkUuid, privateL3.getUuid())
                    .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST)
                    .eq(VmNicVO_.ip, gateways.get(0))
                    .limit(1).findValue();
        }

        if (vrUuid == null) {
            /* vrouter is not created */
            return new ArrayList<>();
        }

        List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.vmInstanceUuid, vrUuid)
                .in(VmNicVO_.metaData, VirtualRouterNicMetaData.ALL_PUBLIC_NIC_MASK_STRING_LIST).listValues();
        return l3Uuids;
    }
}
