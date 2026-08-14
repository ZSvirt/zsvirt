package org.zstack.network.plugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.*;
import org.zstack.kvm.*;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.network.service.flat.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static java.util.Arrays.asList;

/**
 * Created by shixin.ruan on 2021.08.09.
 */
public class FlatGratuitousARPBackend implements GratuitousARPBackend, KVMHostConnectExtensionPoint,
        VmDetachNicExtensionPoint, VmInstanceAttachNicExtensionPoint, VmInstanceMigrateExtensionPoint, VmInstanceStopExtensionPoint, ManagementNodeChangeListener{
    private static final CLogger logger = Utils.getLogger(FlatGratuitousARPBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceDestinationMaker destMaker;

    public static final String APPLY_GRATUITOUS_ARP = "/flatnetworkprovider/garp/apply";
    public static final String RELEASE_GRATUITOUS_ARP = "/flatnetworkprovider/garp/release";
    public static final String CLEANUP_GRATUITOUS_ARP = "/flatnetworkprovider/garp/cleanup";
    public static final String UPDATE_GRATUITOUS_ARP_SETTINGS = "/flatnetworkprovider/garp/settings";

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "configure-garp-info";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                String hostUuid = context.getInventory().getUuid();
                syncState(asList(hostUuid));
                if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
                    logger.debug("grap is disabled");
                    trigger.next();
                    return;
                }

                List<VmInstanceVO> vmVos = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.type, VmInstanceConstant.USER_VM_TYPE)
                        .eq(VmInstanceVO_.hostUuid, hostUuid).list();
                if (vmVos.isEmpty()) {
                    logger.debug(String.format("grap no vm is on host[uuid:%s]", hostUuid));
                    trigger.next();
                    return;
                }

                List<VmNicInventory> nics = new ArrayList<>();
                vmVos.forEach(vo -> nics.addAll(VmNicInventory.valueOf(vo.getVmNics())));

                List<GratuitousARPTO> infos = getInfosFromNics(nics, hostUuid);
                if (infos.isEmpty()) {
                    logger.debug("grap no vmnic is valid");
                    trigger.next();
                    return;
                }
                doApplyGratuitousArp(infos, true, hostUuid);
                trigger.next();
            }
        };
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            return;
        }
        List<GratuitousARPTO> infos = getInfosFromNics(inv.getVmNics(), destHostUuid);
        if (infos.isEmpty()) {
            return;
        }
        doApplyGratuitousArp(infos, false, destHostUuid);
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            return;
        }
        List<GratuitousARPTO> infos = getInfosFromNics(inv.getVmNics(), srcHostUuid);
        if (infos.isEmpty()) {
            return;
        }
        doReleaseGratuitousArp(infos, srcHostUuid);
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            return;
        }
        List<GratuitousARPTO> infos = getInfosFromNics(inv.getVmNics(), destHostUuid);
        if (infos.isEmpty()) {
            return;
        }
        doReleaseGratuitousArp(infos, destHostUuid);
    }

    @Override
    public void preDetachNic(VmNicInventory nic) {

    }

    @Override
    public void beforeDetachNic(VmNicInventory nic) {

    }

    @Override
    public void afterDetachNic(VmNicInventory nic) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            return;
        }
        List<VmNicInventory> nics = new ArrayList<>();
        nics.add(nic);
        List<GratuitousARPTO> infos = getInfosFromNics(nics, getHostUuidFromVmInstance(nic.getVmInstanceUuid()));
        if (infos.isEmpty()) {
            return;
        }
        doReleaseGratuitousArp(infos, getHostUuidFromVmInstance(nic.getVmInstanceUuid()));
    }

    @Override
    public void failedToDetachNic(VmNicInventory nic, ErrorCode error) {

    }

    @Override
    public void afterAttachNicToVm(VmNicInventory nic) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            return;
        }
        List<VmNicInventory> nics = new ArrayList<>();
        nics.add(nic);
        List<GratuitousARPTO> infos = getInfosFromNics(nics, getHostUuidFromVmInstance(nic.getVmInstanceUuid()));
        if (infos.isEmpty()) {
            return;
        }
        doApplyGratuitousArp(infos, false, getHostUuidFromVmInstance(nic.getVmInstanceUuid()));
    }

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            return;
        }
        List<VmNicInventory> nics = new ArrayList<>(inv.getVmNics());
        List<GratuitousARPTO> infos = getInfosFromNics(nics, inv.getLastHostUuid());
        if (infos.isEmpty()) {
            return;
        }
        doReleaseGratuitousArp(infos, inv.getLastHostUuid());
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    public static class GratuitousARPTO {
        public String bridgeName;
        public String namespace;
        public String vmUuid;
        public String mac;
        public String ip;
    }

    public static class CleanupGratuitousARPCmd extends KVMAgentCommands.AgentCommand {
        List<GratuitousARPTO> infos;
    }

    public static class CleanupGratuitousARPRsp extends AgentResponse {

    }

    public static class ApplyGratuitousARPCmd extends KVMAgentCommands.AgentCommand {
        public List<GratuitousARPTO> infos;
        public Boolean rebuild;
        public int interval;
        public boolean state;
    }

    public static class ApplyGratuitousARPRsp extends AgentResponse {

    }

    public static class ReleaseGratuitousARPCmd extends KVMAgentCommands.AgentCommand {
        public List<GratuitousARPTO> infos;
    }

    public static class ReleaseGratuitousARPRsp extends AgentResponse {
    }

    @Override
    public NetworkServiceProviderType getProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void applyGratuitousARP(final GratuitousARPStruct struct, final Completion completion) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            completion.success();
            return;
        }

        String hostUuid = struct.getVm().getHostUuid();
        List<GratuitousARPTO> infos = getInfosFromNics(struct.getVm().getVmNics(), hostUuid);
        if (infos.isEmpty()) {
            completion.success();
            return;
        }

        ApplyGratuitousARPCmd cmd = new ApplyGratuitousARPCmd();
        cmd.infos = infos;
        cmd.rebuild = false;
        cmd.state = GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP;
        cmd.interval = GratuitousARPGlobalConfig.SEND_GRATUITOUS_ARP_INTERVAL.value(Integer.class);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(APPLY_GRATUITOUS_ARP);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                ApplyGratuitousARPRsp rsp = r.toResponse(ApplyGratuitousARPRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("apply gratuitous arp error, because:%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }


    @Override
    public void releaseGratuitousARP(final GratuitousARPStruct struct, final Completion completion) {
        if (!GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP) {
            completion.success();
            return;
        }

        String hostUuid = struct.getVm().getHostUuid() != null ? struct.getVm().getHostUuid() : struct.getVm().getLastHostUuid();
        List<GratuitousARPTO> infos = getInfosFromNics(struct.getVm().getVmNics(), hostUuid);
        if (infos.isEmpty()) {
            completion.success();
            return;
        }

        ReleaseGratuitousARPCmd cmd = new ReleaseGratuitousARPCmd();
        cmd.infos = infos;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(RELEASE_GRATUITOUS_ARP);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                ReleaseGratuitousARPRsp rsp = r.toResponse(ReleaseGratuitousARPRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("release gratuitous arp error, because:%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    private List<GratuitousARPTO> getInfosFromNics(List<VmNicInventory> nics, String hostUuid){
        List<GratuitousARPTO> infos = new ArrayList<>();
        for (VmNicInventory nic : nics) {
            UsedIpInventory ipv4 = null;
            for (UsedIpInventory ip : nic.getUsedIps()) {
                if (ip.getIpVersion() == IPv6Constants.IPv4) {
                    ipv4 = ip;
                }
            }

            /* when destroy vm instance, we can not get nic ip */
            GratuitousARPTO to = new GratuitousARPTO();
            if (ipv4 != null) {
                to.ip = ipv4.getIp();
            }

            to.mac = nic.getMac();
            to.vmUuid = nic.getVmInstanceUuid();
            try {
                to.bridgeName = new BridgeNameFinder().findByL3UuidOnHost(nic.getL3NetworkUuid(), hostUuid);
            } catch (Exception e) {
                logger.debug(String.format("get bridge name failed: %s", e));
                continue;
            }
            to.namespace = FlatDhcpBackend.makeNamespaceName(to.bridgeName, nic.getL3NetworkUuid());

            infos.add(to);
        }

        return infos;
    }

    private String getHostUuidFromVmInstance(String VmInstanceUuid){
        Tuple t =  Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid).eq(VmInstanceVO_.uuid, VmInstanceUuid)
                .findTuple();
        return t.get(0) != null ? t.get(0, String.class) : t.get(1, String.class);
    }

    private void doApplyGratuitousArp(List<GratuitousARPTO> infos, boolean rebuild, String hostUuid){
        ApplyGratuitousARPCmd cmd = new ApplyGratuitousARPCmd();
        cmd.infos = infos;
        cmd.rebuild = rebuild;
        cmd.state = GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP;
        cmd.interval = GratuitousARPGlobalConfig.SEND_GRATUITOUS_ARP_INTERVAL.value(Integer.class);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(APPLY_GRATUITOUS_ARP);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(String.format("failed to send apply gratuitous arp to host[uuid:%s]", hostUuid));
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                ApplyGratuitousARPRsp rsp = r.toResponse(ApplyGratuitousARPRsp.class);
                if (!rsp.isSuccess()) {
                    logger.error(String.format("apply gratuitous arp error, because:%s", rsp.getError()));
                    return;
                }

                logger.debug(String.format("apply gratuitous arp success to host[uuid:%s]", hostUuid));
            }
        });
    }

    private void doReleaseGratuitousArp(List<GratuitousARPTO> infos, String hostUuid){
        ReleaseGratuitousARPCmd cmd = new ReleaseGratuitousARPCmd();
        cmd.infos = infos;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(RELEASE_GRATUITOUS_ARP);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(String.format("failed to send release gratuitous arp to host[uuid:%s]", hostUuid));
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                ReleaseGratuitousARPRsp rsp = r.toResponse(ReleaseGratuitousARPRsp.class);
                if (!rsp.isSuccess()) {
                    logger.error(String.format("release gratuitous arp error, because:%s", rsp.getError()));
                    return;
                }

                logger.debug(String.format("release gratuitous arp success to host[uuid:%s]", hostUuid));
            }
        });
    }

    public void updateGratuitousARPSettings(String newValue, Boolean state, List<String> hostUuids) {
        ApplyGratuitousARPCmd cmd = new ApplyGratuitousARPCmd();
        cmd.interval = Integer.parseInt(newValue);
        List<KVMHostAsyncHttpCallMsg> cmsgs = new ArrayList<>();
        cmd.state = state;
        if (hostUuids == null) {
            hostUuids = Q.New(HostVO.class)
                    .select(HostVO_.uuid)
                    .notEq(HostVO_.status, HostStatus.Disconnected)
                    .listValues();
        }
        for (String uuid : hostUuids) {
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(uuid);
            msg.setCommand(cmd);
            msg.setPath(UPDATE_GRATUITOUS_ARP_SETTINGS);
            msg.setNoStatusCheck(true);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, uuid);
            cmsgs.add(msg);
        }

        if (cmsgs.isEmpty()) {
            return;
        }

        new While<>(cmsgs).step((cMsg, whileCompletion) -> {
            bus.send(cMsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.error(String.format("update gratuitous arp settings error, because:%s", reply.getError()));
                        return;
                    }
                    whileCompletion.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    logger.error(String.format("failed to send update gratuitous arp settings, because:%s", errorCodeList.getCauses().get(0)));
                } else {
                    logger.debug(String.format("update gratuitous arp settings success"));
                }
            }
        });
    }
    @Override
    public void nodeJoin(ManagementNodeInventory inv) {

    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {

    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
        final List<String> finalHostUuids = new ArrayList<>();
        List<String> hostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .notEq(HostVO_.status, HostStatus.Disconnected)
                .listValues();
        hostUuids.forEach( hostUuid -> {
           if (destMaker.isManagedByUs(hostUuid)) {
               finalHostUuids.add(hostUuid);
           }
        });
        syncState(finalHostUuids);
    }

    private void syncState(List<String> hostUuids) {
        //Sync state to KVMAgent at KvmHost connecting, Because KVMAgent will not restart when mn restart
        updateGratuitousARPSettings(GratuitousARPGlobalConfig.SEND_GRATUITOUS_ARP_INTERVAL.value(Integer.class).toString(), GratuitousARPGlobalProperty.SEND_GRATUITOUS_ARP, hostUuids);
    }
}
