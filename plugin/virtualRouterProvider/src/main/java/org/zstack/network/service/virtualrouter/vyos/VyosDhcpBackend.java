package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.compute.vm.VmHostNameHelper;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkSystemTags;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.dhcp.VirtualRouterDhcpBackend;
import org.zstack.network.service.virtualrouter.ha.BeforeCleanUpHaGroupNetworkServiceRefsExtensionPoint;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosDhcpBackend extends VirtualRouterDhcpBackend implements VirtualRouterAfterAttachNicExtensionPoint,
        VirtualRouterBeforeDetachNicExtensionPoint, ApplianceVmSyncConfigToHaGroupExtensionPoint, BeforeCleanUpHaGroupNetworkServiceRefsExtensionPoint {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected VirtualRouterHaBackend haBackend;


    @Override
    public NetworkServiceProviderType getProviderType() {
        return VyosConstants.PROVIDER_TYPE;
    }

    @Override
    protected void acquireVirtualRouterVm(VirtualRouterStruct struct, ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        struct.setApplianceVmType(VyosConstants.VYOS_VM_TYPE);
        struct.setProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        struct.setVirtualRouterOfferingSelector(new VyosOfferingSelector());
        struct.setApplianceVmAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);
        super.acquireVirtualRouterVm(struct, completion);
    }

    @Deferred
    private boolean isDhcpEnabledOnVirtualRouter(String l3Uuid, String vrUuid) {
        // TODO: ui will call APICreateVpcVRouterMsg at same time, we need a lock here
        GLock lock = new GLock(String.format("set-vpc-uuid-for-vyos-dhcp-%s", l3Uuid), TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        String uuid = L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.getTokenByResourceUuid(l3Uuid, L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN);
        if (uuid != null) {
            /* this vr is configured as vpc router for l3Uuid */
            if (uuid.equals(vrUuid)) {
                return true;
            } else {
                /* hagroup of this vr is configured as vpc router for l3Uuid */
                String haUuid = haBackend.getVirtualRouterHaUuid(vrUuid);
                return uuid.equals(haUuid);
            }
        }

        String haUuid = haBackend.getVirtualRouterHaUuid(vrUuid);
        if (haUuid != null) {
            uuid = haUuid;
        } else {
            uuid = vrUuid;
        }

        SystemTagCreator creator = L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.newSystemTagCreator(l3Uuid);
        creator.ignoreIfExisting = false;
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(
                map(
                        e(L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN, uuid)
                )
        );
        creator.create();

        return true;
    }

    protected void refreshDhcpServer(String vrUuid, Completion completion) {
        List<VirtualRouterCommands.DhcpServerInfo> dhcpServerInfos = new ArrayList<>();
        boolean sendEmptyInfo = false;

        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        for (VmNicVO vo : vrVO.getVmNics()) {
            if (VmNicInventory.valueOf(vo).isIpv6OnlyNic()) {
                continue;
            }

            if (!isVRouterDhcpEnabled(vo.getL3NetworkUuid())) {
                continue;
            }

            if (VirtualRouterNicMetaData.isPublicNic(vo) || VirtualRouterNicMetaData.isAddinitionalPublicNic(vo)) {
                if (!isDhcpEnabledOnVirtualRouter(vo.getL3NetworkUuid(), vrUuid)) {
                    sendEmptyInfo = true;
                    continue;
                }
            }

            VirtualRouterCommands.DhcpServerInfo serverInfo = getDhcpServerInfo(VmNicInventory.valueOf(vo), false);
            dhcpServerInfos.add(serverInfo);
        }

        /* there is no network enable vyos dhcp service, skip the flow */
        if (dhcpServerInfos.isEmpty() && !sendEmptyInfo) {
            completion.success();
            return;
        }

        VirtualRouterCommands.RefreshDHCPServerCmd cmd = new VirtualRouterCommands.RefreshDHCPServerCmd();
        cmd.setDhcpServers(dhcpServerInfos);

        VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
        cmsg.setCommand(cmd);
        cmsg.setPath(VirtualRouterConstant.VR_REFRESH_DHCP_SERVER_PATH);
        cmsg.setVmInstanceUuid(vrUuid);
        cmsg.setCheckStatus(false);
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, vrUuid);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.RefreshDHCPServerRsp rsp =  re.toResponse(VirtualRouterCommands.RefreshDHCPServerRsp.class);
                if (rsp.isSuccess()) {
                    completion.success();
                } else {
                    ErrorCode err = operr("unable to start dhcp server on virtual router vm[uuid:%s], because %s", vrUuid, rsp.getError());
                    completion.fail(err);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    protected VirtualRouterCommands.DhcpServerInfo getDhcpServerInfo(VmNicInventory nic, boolean dhcpServerOnly) {
        List<VirtualRouterCommands.DhcpInfo> dhcpInfos = new ArrayList<>();

        L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        VirtualRouterCommands.DhcpServerInfo server = new VirtualRouterCommands.DhcpServerInfo();
        server.setNicMac(nic.getMac());
        server.setSubnet(NetworkUtils.getCidrFromIpMask(nic.getIp(), nic.getNetmask()));
        server.setNetmask(nic.getNetmask());
        server.setGateway(nic.getGateway());
        server.setDnsDomain(l3Vo.getDnsDomain());
        server.setMtu(new MtuGetter().getMtu(nic.getL3NetworkUuid()));
        /* when vyos dhcp is enabled, vm dns request is forwarded by virtual router */
        server.setDnsServer(nic.getIp());

        if (dhcpServerOnly) {
            return server;
        }

        /* get all guest vmNic in the l3 network */
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, nic.getL3NetworkUuid()).notNull(VmNicVO_.ip)
                .isNull(VmNicVO_.metaData).list();
        for (VmNicVO nicVO : vmNicVOS) {
            if (nicVO.getVmInstanceUuid() == null) {
                continue;
            }

            VmInstanceVO vmVO = dbf.findByUuid(nicVO.getVmInstanceUuid(), VmInstanceVO.class);
            if (vmVO == null) {
                continue;
            }

            VirtualRouterCommands.DhcpInfo dhcpInfo = new VirtualRouterCommands.DhcpInfo();
            dhcpInfo.setIp(nicVO.getIp());
            dhcpInfo.setMac(nicVO.getMac());
            if (vmVO.getDefaultL3NetworkUuid().equals(nicVO.getL3NetworkUuid())) {
                dhcpInfo.setDefaultL3Network(true);
            } else {
                dhcpInfo.setDefaultL3Network(false);
            }

            String hostname = new VmHostNameHelper().getHostName(vmVO);
            if (hostname != null && l3Vo.getDnsDomain() != null) {
                hostname = String.format("%s.%s", hostname, l3Vo.getDnsDomain());
            }
            dhcpInfo.setHostname(hostname);

            dhcpInfos.add(dhcpInfo);
        }
        server.setDhcpInfos(dhcpInfos);

        return server;
    }

    protected void startDhcpServer(VmNicInventory nic, Completion completion) {
        if (nic.isIpv6OnlyNic()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.DhcpServerInfo serverInfo = getDhcpServerInfo(nic, false);
        VirtualRouterCommands.RefreshDHCPServerCmd cmd = new VirtualRouterCommands.RefreshDHCPServerCmd();
        cmd.setDhcpServers(Arrays.asList(serverInfo));

        VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
        cmsg.setCommand(cmd);
        cmsg.setPath(VirtualRouterConstant.VR_START_DHCP_SERVER_PATH);
        cmsg.setVmInstanceUuid(nic.getVmInstanceUuid());
        cmsg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.RefreshDHCPServerRsp rsp =  re.toResponse(VirtualRouterCommands.RefreshDHCPServerRsp.class);
                if (rsp.isSuccess()) {
                    completion.success();
                } else {
                    ErrorCode err = operr("unable to start dhcp server on virtual router vm[uuid:%s], because %s", nic.getVmInstanceUuid(), rsp.getError());
                    completion.fail(err);
                }
            }
        });
    }

    protected void stopDhcpServer(VmNicInventory nic, Completion completion) {
        if (nic.isIpv6OnlyNic()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.DhcpServerInfo serverInfo = getDhcpServerInfo(nic, true);
        VirtualRouterCommands.RefreshDHCPServerCmd cmd = new VirtualRouterCommands.RefreshDHCPServerCmd();
        cmd.setDhcpServers(Arrays.asList(serverInfo));

        VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
        cmsg.setCommand(cmd);
        cmsg.setPath(VirtualRouterConstant.VR_STOP_DHCP_SERVER_PATH);
        cmsg.setVmInstanceUuid(nic.getVmInstanceUuid());
        cmsg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.RefreshDHCPServerRsp rsp =  re.toResponse(VirtualRouterCommands.RefreshDHCPServerRsp.class);
                if (rsp.isSuccess()) {
                    completion.success();
                } else {
                    ErrorCode err = operr("unable to stop dhcp server on virtual router vm[uuid:%s], because %s", nic.getVmInstanceUuid(), rsp.getError());
                    completion.fail(err);
                }
            }
        });
    }

    boolean isDhcpEnabledOnNic(VmNicInventory nic) {
        boolean enableDhcp = isVRouterDhcpEnabled(nic.getL3NetworkUuid());

        if (!enableDhcp) {
            return false;
        }

        if (VirtualRouterNicMetaData.isPublicNic(nic) || VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
            if (!isDhcpEnabledOnVirtualRouter(nic.getL3NetworkUuid(), nic.getVmInstanceUuid())) {
                return false;
            }
        }

        return true;
    }


    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!isDhcpEnabledOnNic(nic)) {
            completion.success();
            return;
        }

        startDhcpServer(nic, completion);
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        if (!isDhcpEnabledOnNic(nic)) {
            completion.done();
            return;
        }

        stopDhcpServer(nic, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });

    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (!isDhcpEnabledOnNic(nic)) {
            completion.success();
            return;
        }

        stopDhcpServer(nic, completion);
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        if (!isDhcpEnabledOnNic(nic)) {
            completion.done();
            return;
        }

        startDhcpServer(nic, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {
        for (VmNicInventory nic : inv.getVmNics()) {
            boolean enableDhcp = isVRouterDhcpEnabled(nic.getL3NetworkUuid());

            if (!enableDhcp) {
                continue;
            }

            String uuid = L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.getTokenByResourceUuid(nic.getL3NetworkUuid(), L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN);
            if (!inv.getUuid().equals(uuid)) {
                continue;
            }

            L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.updateTagByToken(nic.getL3NetworkUuid(),
                    L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN, haUuid);
        }
    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {
        for (VmNicInventory nic : inv.getVmNics()) {
            boolean enableDhcp = isVRouterDhcpEnabled(nic.getL3NetworkUuid());

            if (!enableDhcp) {
                continue;
            }

            String uuid = L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.getTokenByResourceUuid(nic.getL3NetworkUuid(), L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN);
            if (!haUuid.equals(uuid)) {
                continue;
            }

            L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.updateTagByToken(nic.getL3NetworkUuid(),
                    L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN, inv.getUuid());
        }
    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void beforeCleanUp(VmInstanceInventory vrInv) {
        String haUuid = haBackend.getVirtualRouterHaUuid(vrInv.getUuid());
        if (haUuid == null) {
            return;
        }

        for (VmNicInventory nic : vrInv.getVmNics()) {
            String uuid = L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.getTokenByResourceUuid(nic.getL3NetworkUuid(), L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN);
            if (uuid == null) {
                continue;
            }

            if (haUuid.equals(uuid)) {
                L3NetworkSystemTags.PUBLIC_NETWORK_DHCP_SERVER_UUID.delete(nic.getL3NetworkUuid());
            }
        }
    }
}
