package org.zstack.network.service.virtualrouter.dns;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.network.service.virtualrouter.vyos.VyosOfferingSelector;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by AlanJager on 2017/7/8.
 */
public class VirtualRouterCentralizedDnsBackend extends AbstractVirtualRouterBackend implements NetworkServiceCentralizedDnsBackend,
        VmInstanceMigrateExtensionPoint, DnsServiceExtensionPoint {
    private final CLogger logger = Utils.getLogger(VirtualRouterCentralizedDnsBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private NetworkServiceManager nsMgr;

    public static final String SET_DNS_FORWARD_PATH = "/dns/forward/set";
    public static final String REMOVE_DNS_FORWARD_PATH = "/dns/forward/remove";

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    public static String makeNamespaceName(String brName, String l3Uuid) {
        return String.format("%s_%s", brName, l3Uuid);
    }

    private List<ForwardDnsStruct> beforeDoService(List<ForwardDnsStruct> forwardDnsStructs) {
        Iterator<ForwardDnsStruct> iterator = forwardDnsStructs.iterator();

        while (iterator.hasNext()) {
            boolean shouldRemove = false;
            for (NetworkServiceL3NetworkRefInventory ref : iterator.next().getL3Network().getNetworkServices()) {
                if (!ref.getNetworkServiceType().equals(NetworkServiceType.DHCP.toString())) {
                    continue;
                }

                NetworkServiceProviderVO vo = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.uuid, ref.getNetworkServiceProviderUuid()).find();
                if (vo.getType().equals(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE) || vo.getType().equals(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE)) {
                    shouldRemove = true;
                }
            }

            if (shouldRemove) {
                iterator.remove();
            }
        }

        return forwardDnsStructs;
    }

    @Override
    public void applyForwardDnsService(List<ForwardDnsStruct> forwardDnsStructs, VmInstanceSpec spec, Completion completion) {
        forwardDnsStructs = beforeDoService(forwardDnsStructs);

        if (forwardDnsStructs.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(forwardDnsStructs).each((forwardDnsStruct, whileComplection) -> {
            VirtualRouterStruct s = new VirtualRouterStruct();
            s.setL3Network(forwardDnsStruct.getL3Network());
            s.setApplianceVmType(VyosConstants.VYOS_VM_TYPE);
            s.setProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
            s.setVirtualRouterOfferingSelector(new VyosOfferingSelector());
            s.setApplianceVmAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);

            vrMgr.acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(whileComplection) {
                @Override
                public void success(VirtualRouterVmInventory vr) {
                    VirtualRouterCommands.SetForwardDnsCmd cmd = new VirtualRouterCommands.SetForwardDnsCmd();
                    cmd.setMac(forwardDnsStruct.getMac());
                    String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                            " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                            .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                            .param("l3Uuid", forwardDnsStruct.getL3Network().getUuid())
                            .param("ttype", L2NetworkVO.class.getSimpleName())
                            .find();
                    cmd.setBridgeName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
                    cmd.setNameSpace(makeNamespaceName(
                            cmd.getBridgeName(),
                            forwardDnsStruct.getL3Network().getUuid()
                    ));
                    for (VmNicInventory nic : vr.getVmNics()) {
                        if (nic.getL3NetworkUuid().equals(forwardDnsStruct.getL3Network().getUuid())) {
                            cmd.setDns(nic.getIp());
                        }
                    }
                    cmd.setWrongDns(forwardDnsStruct.getL3Network().getDns());

                    KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
                    kmsg.setCommand(cmd);
                    kmsg.setPath(SET_DNS_FORWARD_PATH);
                    kmsg.setHostUuid(spec.getDestHost().getUuid());
                    bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
                    bus.send(kmsg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            VirtualRouterCommands.SetForwardDnsRsp rsp = !reply.isSuccess() ? null :
                                    ((KVMHostAsyncHttpCallReply) reply).toResponse(VirtualRouterCommands.SetForwardDnsRsp.class);
                            if (!reply.isSuccess() || !rsp.isSuccess()) {
                                logger.warn(String.format("set forwarding error on host[uuid:%s]", spec.getVmInventory().getHostUuid()));
                            }

                            whileComplection.done();

                        }
                    });
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileComplection.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public void releaseForwardDnsService(List<ForwardDnsStruct> forwardDnsStructs, VmInstanceSpec spec, NoErrorCompletion completion) {
        forwardDnsStructs = beforeDoService(forwardDnsStructs);

        if (forwardDnsStructs.isEmpty()) {
            completion.done();
            return;
        }

        new While<>(forwardDnsStructs).each((forwardDnsStruct, whileComplection) -> {
            VirtualRouterCommands.RemoveForwardDnsCmd cmd = new VirtualRouterCommands.RemoveForwardDnsCmd();
            cmd.setMac(forwardDnsStruct.getMac());
            String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                    " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                    .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                    .param("l3Uuid", forwardDnsStruct.getL3Network().getUuid())
                    .param("ttype", L2NetworkVO.class.getSimpleName())
                    .find();
            cmd.setBridgeName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
            cmd.setNameSpace(makeNamespaceName(
                    cmd.getBridgeName(),
                    forwardDnsStruct.getL3Network().getUuid()
            ));

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setCommand(cmd);
            kmsg.setPath(REMOVE_DNS_FORWARD_PATH);
            kmsg.setHostUuid(spec.getDestHost().getUuid());
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
            bus.send(kmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    VirtualRouterCommands.RemoveForwardDnsRsp rsp = !reply.isSuccess() ? null :
                            ((KVMHostAsyncHttpCallReply) reply).toResponse(VirtualRouterCommands.RemoveForwardDnsRsp.class);
                    if (!reply.isSuccess() || !rsp.isSuccess()) {
                        logger.warn(String.format("set forwarding error on host[uuid:%s]", spec.getVmInventory().getHostUuid()));
                    }

                    whileComplection.done();

                }
            });

        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        if (inv.getDefaultL3NetworkUuid() == null) {
            return;
        }

        L3NetworkVO defaultL3 = dbf.findByUuid(inv.getDefaultL3NetworkUuid(), L3NetworkVO.class);
        if (!defaultL3.getNetworkServices().stream().map(NetworkServiceL3NetworkRefVO::getNetworkServiceType)
                .collect(Collectors.toList()).contains(NetworkServiceType.Centralized_DNS.toString())) {
            return;
        }

        NetworkServiceProviderType providerType = null;
        try {
            providerType = nsMgr.getTypeOfNetworkServiceProviderForService(defaultL3.getUuid(), NetworkServiceType.DHCP);
        } catch (Throwable e){
            logger.warn(e.getMessage(), e);
            return;
        }
        if (VyosConstants.PROVIDER_TYPE.equals(providerType)) {
            return;
        }

        L3NetworkInventory defaultL3Inv = L3NetworkInventory.valueOf(defaultL3);
        /* install dns forward address for default network */
        VirtualRouterCommands.SetForwardDnsCmd cmd = new VirtualRouterCommands.SetForwardDnsCmd();
        for (VmNicInventory nic : inv.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(inv.getDefaultL3NetworkUuid())) {
                cmd.setMac(nic.getMac());
            }
        }
        String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                .param("l3Uuid", inv.getDefaultL3NetworkUuid())
                .param("ttype", L2NetworkVO.class.getSimpleName())
                .find();
        cmd.setBridgeName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
        cmd.setNameSpace(makeNamespaceName(
                cmd.getBridgeName(),
                inv.getDefaultL3NetworkUuid()
        ));

        /* TODO: ipv6 dns doesn't need this api */
        List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(defaultL3Inv, IPv6Constants.IPv4);
        if (!iprs.isEmpty()) {
            cmd.setDns(iprs.get(0).getGateway());
        }
        cmd.setWrongDns(defaultL3Inv.getDns());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setPath(SET_DNS_FORWARD_PATH);
        kmsg.setHostUuid(destHostUuid);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, destHostUuid);
        bus.send(kmsg);
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (inv.getDefaultL3NetworkUuid() == null) {
            return;
        }
        
        L3NetworkVO defaultL3 = dbf.findByUuid(inv.getDefaultL3NetworkUuid(), L3NetworkVO.class);
        if (!defaultL3.getNetworkServices().stream().map(NetworkServiceL3NetworkRefVO::getNetworkServiceType)
                .collect(Collectors.toList()).contains(NetworkServiceType.Centralized_DNS.toString())) {
            return;
        }

        /* uninstall dns forward address for default network */
        VirtualRouterCommands.RemoveForwardDnsCmd cmd = new VirtualRouterCommands.RemoveForwardDnsCmd();
        for (VmNicInventory nic : inv.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(inv.getDefaultL3NetworkUuid())) {
                cmd.setMac(nic.getMac());
            }
        }
        String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                .param("l3Uuid", inv.getDefaultL3NetworkUuid())
                .param("ttype", L2NetworkVO.class.getSimpleName())
                .find();
        cmd.setBridgeName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
        cmd.setNameSpace(makeNamespaceName(
                cmd.getBridgeName(),
                inv.getDefaultL3NetworkUuid()
        ));

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setPath(REMOVE_DNS_FORWARD_PATH);
        kmsg.setHostUuid(destHostUuid);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, destHostUuid);
        bus.send(kmsg);
    }

    @Override
    public List<String> getDnsAddress(L3NetworkInventory inv) {
        List<String> dns = new ArrayList<>();

        if (!inv.getType().equals(L3NetworkConstant.L3_BASIC_NETWORK_TYPE)) {
            return dns;
        }

        /* only virtual router network will add gateway as dns address */
        for (NetworkServiceL3NetworkRefInventory ref : inv.getNetworkServices()) {
            if (!ref.getNetworkServiceType().equals(NetworkServiceType.SNAT.toString())) {
                continue;
            }

            /* virtual router doesn't has ipv6 */
            List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(inv, IPv6Constants.IPv4);
            if (iprs.isEmpty()) {
                continue;
            }

            NetworkServiceProviderVO vo = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.uuid, ref.getNetworkServiceProviderUuid()).find();
            if (vo.getType().equals(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE) || vo.getType().equals(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE)) {
                dns.add(iprs.get(0).getGateway());
            }
        }

        return dns;
    }
}
