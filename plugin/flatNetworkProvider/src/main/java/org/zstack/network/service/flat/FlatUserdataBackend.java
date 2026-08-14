package org.zstack.network.service.flat;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.UserdataBuilder;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkUpdateExtensionPoint;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.network.service.NetworkProviderFinder;
import org.zstack.network.service.NetworkServiceFilter;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.userdata.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 10/13/2015.
 */
public class FlatUserdataBackend implements UserdataBackend, KVMHostConnectExtensionPoint,
        L3NetworkDeleteExtensionPoint, L2NetworkUpdateExtensionPoint, VmInstanceMigrateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatUserdataBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private FlatDhcpBackend dhcpBackend;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private NetworkServiceManager nsMgr;

    public static final String APPLY_USER_DATA = "/flatnetworkprovider/userdata/apply";
    public static final String BATCH_APPLY_USER_DATA = "/flatnetworkprovider/userdata/batchapply";
    public static final String RELEASE_USER_DATA = "/flatnetworkprovider/userdata/release";
    public static final String CLEANUP_USER_DATA = "/flatnetworkprovider/userdata/cleanup";

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return createHostPrepareUserdataFlow(context.getInventory().getUuid());
    }

    private Flow createHostPrepareUserdataFlow(final String hostUuid) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-userdata";

            @Transactional(readOnly = true)
            private List<String> getVmsNeedUserdataOnHost() {
                String sql = "select vm.uuid from VmInstanceVO vm where vm.hostUuid = :huuid and vm.state = :state and vm.type = :type";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("state", VmInstanceState.Running);
                q.setParameter("huuid", hostUuid);
                q.setParameter("type", VmInstanceConstant.USER_VM_TYPE);
                List<String> vmUuids = q.getResultList();
                if (vmUuids.isEmpty()) {
                    return null;
                }

                vmUuids = new NetworkServiceFilter().filterVmByServiceTypeAndProviderType(vmUuids, UserdataConstant.USERDATA_TYPE_STRING, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
                if (vmUuids.isEmpty()) {
                    return null;
                }

                return vmUuids;
            }

            class VmIpL3Uuid {
                String vmIp;
                String netmask;
                String l3Uuid;
                String dhcpServerIp;
            }

            @Transactional(readOnly = true)
            private Map<String, VmIpL3Uuid> getVmIpL3Uuid(List<String> vmUuids) {
                String sql = "select vm.uuid, ip.ip, ip.l3NetworkUuid, ip.netmask from VmInstanceVO vm," +
                        "VmNicVO nic, NetworkServiceL3NetworkRefVO ref," +
                        "NetworkServiceProviderVO pro, UsedIpVO ip where " +
                        " vm.uuid = nic.vmInstanceUuid and vm.uuid in (:uuids)" +
                        " and nic.uuid = ip.vmNicUuid " +
                        " and ip.l3NetworkUuid = vm.defaultL3NetworkUuid and ip.ipVersion = :ipversion" +
                        " and ref.networkServiceProviderUuid = pro.uuid" +
                        " and ref.l3NetworkUuid = vm.defaultL3NetworkUuid" +
                        " and pro.type = :proType";

                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("uuids", vmUuids);
                q.setParameter("proType", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
                /* current only ipv4 has userdata */
                q.setParameter("ipversion", IPv6Constants.IPv4);
                List<Tuple> ts = q.getResultList();

                Map<String, VmIpL3Uuid> ret = new HashMap<String, VmIpL3Uuid>();
                for (Tuple t : ts) {
                    String vmUuid = t.get(0, String.class);
                    VmIpL3Uuid v = new VmIpL3Uuid();
                    v.vmIp = t.get(1, String.class);
                    v.l3Uuid = t.get(2, String.class);
                    v.netmask = t.get(3, String.class);
                    ret.put(vmUuid, v);
                }

                return ret;
            }

            private List<UserdataTO> getUserData() {
                List<String> vmUuids = getVmsNeedUserdataOnHost();
                if (vmUuids == null) {
                    return null;
                }

                Map<String, VmIpL3Uuid> vmipl3 = getVmIpL3Uuid(vmUuids);
                if (vmipl3.isEmpty()) {
                    return null;
                }

                // filter out vm that not using flat network provider
                vmUuids = vmUuids.stream().filter(vmipl3::containsKey).collect(Collectors.toList());
                if (vmUuids.isEmpty()) {
                    return null;
                }

                Map<String, List<String>> userdata = new UserdataBuilder().buildByVmUuids(vmUuids);
                Set<String> l3Uuids = new HashSet<String>();
                for (VmIpL3Uuid l : vmipl3.values()) {
                    String dhcpIp = dhcpBackend.allocateDhcpIp(l.l3Uuid, IPv6Constants.IPv4);
                    if (dhcpIp != null) {
                        l.dhcpServerIp = dhcpIp;
                    }
                    l3Uuids.add(l.l3Uuid);
                }
                List<UserdataTO> tos = new ArrayList<>();
                if (l3Uuids.isEmpty()) {
                    return tos;
                }
                Map<String, String> bridgeNames = new BridgeNameFinder().findByL3UuidsOnHost(l3Uuids, hostUuid);
                List<String> l2Uuids = Q.New(L3NetworkVO.class)
                        .select(L3NetworkVO_.l2NetworkUuid).in(L3NetworkVO_.uuid, l3Uuids).listValues();
                Map<String, String> virtualNetworkIdMap = new BridgeVirtualNetworkIdFinder().findByL2Uuids(l2Uuids, hostUuid);

                for (String vmuuid : vmUuids) {
                    UserdataTO to = new UserdataTO();
                    MetadataTO mto = new MetadataTO();
                    mto.vmUuid = vmuuid;
                    mto.vmHostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vmuuid, VmSystemTags.HOSTNAME_TOKEN);
                    to.metadata = mto;

                    VmIpL3Uuid l = vmipl3.get(vmuuid);
                    if (l.vmIp == null) {
                        continue;
                    }

                    if (bridgeNames.get(l.l3Uuid) == null) {
                        continue;
                    }

                    if (l.netmask == null) {
                        /* user data config need netmask */
                        logger.info(String.format("can not get netmask for vmnic[ip:%s] for vm[uuid:%s]",
                                l.vmIp, vmuuid));
                        continue;
                    }

                    to.dhcpServerIp = l.dhcpServerIp;
                    to.vmIp = l.vmIp;
                    to.netmask = l.netmask;
                    to.bridgeName = bridgeNames.get(l.l3Uuid);
                    to.virtualNetworkId = virtualNetworkIdMap.get(to.bridgeName);
                    to.namespaceName = FlatDhcpBackend.makeNamespaceName(to.bridgeName, l.l3Uuid);
                    if (userdata.get(vmuuid) != null) {
                        to.userdataList.addAll(userdata.get(vmuuid));
                    }
                    to.port = UserdataGlobalProperty.HOST_PORT;
                    to.l3NetworkUuid = l.l3Uuid;
                    to.agentConfig = new HashMap<>();
                    for (BeforeUpdateUserdataExtensionPoint ext : pluginRgty.getExtensionList(BeforeUpdateUserdataExtensionPoint.class)) {
                        ext.beforeApplyUserdata(vmuuid, to);
                    }
                    tos.add(to);
                }

                return tos;
            }

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<UserdataTO> tos = getUserData();
                if (tos == null) {
                    trigger.next();
                    return;
                }

                BatchApplyUserdataCmd cmd = new BatchApplyUserdataCmd();
                cmd.userdata = tos;
                cmd.rebuild = true;

                new KvmCommandSender(hostUuid, true).send(cmd, BATCH_APPLY_USER_DATA, new KvmCommandFailureChecker() {
                    @Override
                    public ErrorCode getError(KvmResponseWrapper wrapper) {
                        AgentResponse rsp = wrapper.getResponse(AgentResponse.class);
                        return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
                    }
                }, new ReturnValueCompletion<KvmResponseWrapper>(trigger) {
                    @Override
                    public void success(KvmResponseWrapper returnValue) {
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
    public String preDeleteL3Network(L3NetworkInventory inventory) {
        return null;
    }

    @Override
    public void beforeDeleteL3Network(L3NetworkInventory inventory) {
    }

    @Override
    public void afterDeleteL3Network(L3NetworkInventory l3) {
        Optional<NetworkServiceL3NetworkRefInventory> o = l3.getNetworkServices().stream().filter(n -> n.getNetworkServiceType().equals(UserdataConstant.USERDATA_TYPE_STRING)).findAny();
        if (!o.isPresent()) {
            return;
        }

        NetworkServiceL3NetworkRefInventory ref = o.get();
        if (!dbf.isExist(ref.getNetworkServiceProviderUuid(), NetworkServiceProviderVO.class)) {
            return;
        }

        List<String> hostUuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select h.uuid from HostVO h, L2NetworkClusterRefVO ref where h.clusterUuid = ref.clusterUuid" +
                        " and ref.l2NetworkUuid = :l2Uuid and h.hypervisorType = :hvType";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("l2Uuid", l3.getL2NetworkUuid());
                q.setParameter("hvType", KVMConstant.KVM_HYPERVISOR_TYPE);
                return q.getResultList();
            }
        }.call();

        if (hostUuids.isEmpty()) {
            return;
        }

        for (String hostUuid : hostUuids) {
            CleanupUserdataCmd cmd = new CleanupUserdataCmd();
            cmd.bridgeName = new BridgeNameFinder().findByL3UuidOnHost(l3.getUuid(), hostUuid);
            cmd.l3NetworkUuid = l3.getUuid();
            cmd.namespaceName = FlatDhcpBackend.makeNamespaceName(cmd.bridgeName, cmd.l3NetworkUuid);
            new KvmCommandSender(hostUuid).send(cmd, CLEANUP_USER_DATA, new KvmCommandFailureChecker() {
                @Override
                public ErrorCode getError(KvmResponseWrapper w) {
                    CleanupUserdataRsp rsp = w.getResponse(CleanupUserdataRsp.class);
                    return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
                }
            }, new ReturnValueCompletion<KvmResponseWrapper>(null) {
                @Override
                public void success(KvmResponseWrapper w) {
                    logger.debug(String.format("successfully cleanup userdata service on the host[uuid:%s] for the deleted L3 network[uuid:%s, name:%s]",
                            hostUuid, l3.getUuid(), l3.getName()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    //TODO: Add GC
                    logger.warn(errorCode.getReadableDetails());
                }
            });
        }
    }

    private UserdataStruct makeUserdataStructForMigratingVm(VmInstanceInventory inv, String hostUuid) {
        if (!nsMgr.isVmNeedNetworkService(inv.getType(), UserdataConstant.USERDATA_TYPE)) {
            return null;
        }

        String providerType = new NetworkProviderFinder().getNetworkProviderTypeByNetworkServiceType(inv.getDefaultL3NetworkUuid(), UserdataConstant.USERDATA_TYPE_STRING);
        if (!FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING.equals(providerType)) {
            return null;
        }

        List<String> userdataList = new UserdataBuilder().buildByVmUuid(inv.getUuid());
        if (userdataList == null) {
            // Apply userdata anyway after migrating VM to redeploy lighttpd server
            // so that the VM can still send metric data to pushgateway through lighttpd
            userdataList = Collections.emptyList();
        }

        UserdataStruct struct = new UserdataStruct();
        struct.setParametersFromVmInventory(inv);
        struct.setHostUuid(hostUuid);
        struct.setL3NetworkUuid(inv.getDefaultL3NetworkUuid());
        struct.setUserdataList(userdataList);
        return struct;
    }


    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        UserdataStruct struct = makeUserdataStructForMigratingVm(inv, destHostUuid);
        if (struct == null) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        applyUserdata(struct, completion);
        completion.await();

        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    public static class UserdataReleseGC extends TimeBasedGarbageCollector {
        public static long INTERVAL = 300;

        @GC
        public UserdataStruct struct;

        @Override
        protected void triggerNow(GCCompletion completion) {

            HostStatus status = Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, struct.getHostUuid()).findValue();
            if (status == null) {
                // host deleted
                completion.cancel();
                return;
            }

            if (status != HostStatus.Connected) {
                completion.fail(operr("host[uuid:%s] is not connected", struct.getHostUuid()));
                return;
            }

            VmNicInventory selected = CollectionUtils.findOneOrNull(struct.getVmNics(),
                    arg -> arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()));
            if (selected == null) {
                completion.cancel();
                return;
            }

            ReleaseUserdataCmd cmd = new ReleaseUserdataCmd();
            cmd.hostUuid = struct.getHostUuid();
            cmd.bridgeName = new BridgeNameFinder().findByL3UuidOnHost(struct.getL3NetworkUuid(), struct.getHostUuid());
            cmd.namespaceName = FlatDhcpBackend.makeNamespaceName(cmd.bridgeName, struct.getL3NetworkUuid());
            cmd.vmIp = selected.getIp();

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(struct.getHostUuid());
            msg.setCommand(cmd);
            msg.setPath(RELEASE_USER_DATA);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getHostUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }

                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    ReleaseUserdataRsp rsp = r.toResponse(ReleaseUserdataRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                        return;
                    }

                    completion.success();
                }
            });
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        UserdataStruct struct = makeUserdataStructForMigratingVm(inv, srcHostUuid);
        if (struct == null) {
            return;
        }

        releaseUserdata(struct, new Completion(null) {
            @Override
            public void success() {
                // nothing
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release userdata on the source host[uuid:%s]" +
                        " for the migrated VM[uuid: %s, name:%s], %s. GC will take care it", srcHostUuid, inv.getUuid(), inv.getName(), errorCode));
                UserdataReleseGC gc = new UserdataReleseGC();
                gc.struct = struct;
                gc.submit(UserdataReleseGC.INTERVAL, TimeUnit.SECONDS);
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (destHostUuid == null) {
            return;
        }

        UserdataStruct struct = makeUserdataStructForMigratingVm(inv, destHostUuid);
        if (struct == null) {
            return;
        }

        // clean the userdata that set on the dest host
        releaseUserdata(struct, new Completion(null) {
            @Override
            public void success() {
                // nothing
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UserdataReleseGC gc = new UserdataReleseGC();
                gc.struct = struct;
                gc.submit(UserdataReleseGC.INTERVAL, TimeUnit.SECONDS);
            }
        });
    }

    public static class UserdataTO {
        public MetadataTO metadata;
        public List<String> userdataList = new ArrayList<>();
        public String vmIp;
        public String netmask;
        public String dhcpServerIp;
        public String bridgeName;
        public String virtualNetworkId;
        public String namespaceName;
        public String l3NetworkUuid;
        public Map<String, String> agentConfig;
        public int port;
    }

    public static class MetadataTO {
        public String vmUuid;
        public String vmHostname;
    }

    public static class CleanupUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
        public String l3NetworkUuid;
        public String namespaceName;
    }

    public static class CleanupUserdataRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class BatchApplyUserdataCmd extends KVMAgentCommands.AgentCommand {
        public List<UserdataTO> userdata;
        public boolean rebuild;
    }

    public static class ApplyUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        public UserdataTO userdata;
    }

    public static class ApplyUserdataRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class ReleaseUserdataCmd extends KVMAgentCommands.AgentCommand {
        public String hostUuid;
        public String vmIp;
        public String bridgeName;
        public String namespaceName;
    }

    public static class ReleaseUserdataRsp extends KVMAgentCommands.AgentResponse {
    }

    @Override
    public NetworkServiceProviderType getProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    private boolean hasMetedata(UserdataStruct struct) {
        return VmSystemTags.HOSTNAME.getTokenByResourceUuid(struct.getVmUuid(), VmSystemTags.HOSTNAME_TOKEN) != null;
    }

    private boolean hasUserdata(UserdataStruct struct) {
        return struct.getUserdataList() != null && !struct.getUserdataList().isEmpty();
    }

    @Override
    public void applyUserdata(final UserdataStruct struct, final Completion completion) {
        if (!UserdataGlobalConfig.OPEN_USERDATA_SERVICE_BY_DEFAULT.value(Boolean.class)) {
            if ( !hasMetedata(struct) && !hasUserdata(struct)) {
                completion.success();
                return;
            }
        }

        if (new FlatNetworkServiceValidator().validate(struct.getHostUuid())) {
            completion.success();
            return;
        }

        VmNicInventory destNic = CollectionUtils.findOneOrNull(struct.getVmNics(),
                arg -> arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()));

        if (destNic == null) {
            completion.success();
            return;
        }

        L3NetworkVO defaultL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, struct.getL3NetworkUuid()).find();
        if (defaultL3 == null) {
            completion.success();
            return;
        }

        if (defaultL3.getNetworkServices().stream().noneMatch(
                        service -> Objects.equals(UserdataConstant.USERDATA_TYPE_STRING, service.getNetworkServiceType()))) {
            completion.success();
            return;
        }

        UsedIpVO ipv4 = Q.New(UsedIpVO.class).eq(UsedIpVO_.vmNicUuid, destNic.getUuid())
                .eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4).limit(1).find();
        if (ipv4 == null) {
            // userdata depends on the ipv4 address
            completion.success();
            return;
        }
        if (StringUtils.isEmpty(ipv4.getNetmask())) {
            // userdata depends on the ipv4 netmask
            logger.info(String.format("can not get netmask for vmnic[ip:%s] for vm[uuid:%s]",
                    ipv4.getIp(), struct.getVmUuid()));
            completion.success();
            return;
        }


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("flat-network-userdata-set-for-vm-%s", struct.getVmUuid()));
        chain.then(new ShareFlow() {
            String dhcpServerIp;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-dhcp-server-ip";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        FlatDhcpAcquireDhcpServerIpMsg msg = new FlatDhcpAcquireDhcpServerIpMsg();
                        msg.setL3NetworkUuid(struct.getL3NetworkUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, struct.getL3NetworkUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                FlatDhcpAcquireDhcpServerIpReply dreply = (FlatDhcpAcquireDhcpServerIpReply) reply;
                                if (dreply.getDhcpServerList() != null && !dreply.getDhcpServerList().isEmpty()) {
                                    dhcpServerIp = dreply.getDhcpServerList().get(0).getIp();
                                }


                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "apply-user-data";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ApplyUserdataCmd cmd = new ApplyUserdataCmd();
                        cmd.hostUuid = struct.getHostUuid();

                        MetadataTO to = new MetadataTO();
                        to.vmUuid = struct.getVmUuid();
                        to.vmHostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(struct.getVmUuid(), VmSystemTags.HOSTNAME_TOKEN);
                        UserdataTO uto = new UserdataTO();
                        uto.metadata = to;
                        uto.userdataList = struct.getUserdataList();
                        uto.dhcpServerIp = dhcpServerIp;
                        uto.vmIp = ipv4.getIp();
                        uto.netmask = ipv4.getNetmask();
                        uto.bridgeName = new BridgeNameFinder().findByL3UuidOnHost(struct.getL3NetworkUuid(), struct.getHostUuid());
                        uto.virtualNetworkId = new BridgeVirtualNetworkIdFinder()
                                .findByL2Uuid(defaultL3.getL2NetworkUuid(), struct.getHostUuid(),false);
                        uto.namespaceName = FlatDhcpBackend.makeNamespaceName(uto.bridgeName, struct.getL3NetworkUuid());
                        uto.port = UserdataGlobalProperty.HOST_PORT;
                        uto.l3NetworkUuid = struct.getL3NetworkUuid();
                        uto.agentConfig = new HashMap<>();
                        for (BeforeUpdateUserdataExtensionPoint ext : pluginRgty.getExtensionList(BeforeUpdateUserdataExtensionPoint.class)) {
                            ext.beforeApplyUserdata(struct.getVmUuid(), uto);
                        }
                        cmd.userdata = uto;

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getHostUuid());
                        msg.setCommand(cmd);
                        msg.setPath(APPLY_USER_DATA);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply r = reply.castReply();
                                ApplyUserdataRsp rsp = r.toResponse(ApplyUserdataRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    return;
                                }

                                trigger.next();
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
    public void releaseUserdata(final UserdataStruct struct, final Completion completion) {
        if (new FlatNetworkServiceValidator().validate(struct.getHostUuid())) {
            completion.success();
            return;
        }


        VmNicInventory destNic = CollectionUtils.findOneOrNull(struct.getVmNics(),
                arg -> arg.getL3NetworkUuid().equals(struct.getL3NetworkUuid()));

        if (destNic == null) {
            completion.success();
            return;
        }

        UsedIpInventory ipv4 = destNic.getUsedIps().stream().filter(ip -> ip.getIpVersion() == IPv6Constants.IPv4).findAny().orElse(null);
        if (ipv4 == null) {
            // userdata depends on the ipv4 address
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("flat-network-userdata-release-for-vm-%s", struct.getVmUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "release-user-data";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ReleaseUserdataCmd cmd = new ReleaseUserdataCmd();
                        cmd.hostUuid = struct.getHostUuid();
                        cmd.bridgeName = new BridgeNameFinder().findByL3UuidOnHost(struct.getL3NetworkUuid(), struct.getHostUuid());
                        cmd.namespaceName = FlatDhcpBackend.makeNamespaceName(cmd.bridgeName, struct.getL3NetworkUuid());
                        cmd.vmIp = ipv4.getIp();

                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setHostUuid(struct.getHostUuid());
                        msg.setCommand(cmd);
                        msg.setPath(RELEASE_USER_DATA);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, struct.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                KVMHostAsyncHttpCallReply r = reply.castReply();
                                ReleaseUserdataRsp rsp = r.toResponse(ReleaseUserdataRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    return;
                                }

                                trigger.next();
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
    public void beforeUpdateL2NetworkVirtualNetworkId(L2NetworkInventory l2Inv) {

    }

    @Override
    public void afterUpdateL2NetworkVirtualNetworkId(L2NetworkInventory l2Inv) {
        new While<>(L2NetworkHostUtils.getHostsByAttachedL2Network(l2Inv)).step((host, whileCompletion) -> {
            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName(String.format("after-l2-%s-updated-flat-network-userdata-apply-in-host-%s",
                    l2Inv.getUuid(), host.getUuid()));
            chain.then(createHostPrepareUserdataFlow(host.getUuid()));
            chain.done(new FlowDoneHandler(whileCompletion) {
                @Override
                public void handle(Map data) {
                    whileCompletion.done();
                }
            }).error(new FlowErrorHandler(whileCompletion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    whileCompletion.addError(errCode);
                    whileCompletion.allDone();
                }
            }).start();
        },10).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    logger.error(String.format("apply userdata failed after L2Network updated, because:%s",
                            errorCodeList.getCauses().get(0)));
                } else {
                    logger.debug("apply userdata successful after L2Network updated");
                }
            }
        });
    }
}
