package org.zstack.network.l2.virtualSwitch;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.host.NetworkInterfaceType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.kvm.*;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.network.l3.NetworkGlobalProperty;
import org.zstack.network.service.MtuGetter;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class KVMRealizeL2NoVlanLinuxBridgeBackend implements L2NetworkRealizationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMRealizeL2NoVlanLinuxBridgeBackend.class);
    protected static final L2ProviderType l2ProviderType = new L2ProviderType(KVMConstant.L2_PROVIDER_TYPE_LINUX_BRIDGE);

    @Autowired
    protected CloudBus bus;

    protected String getInterfaceName(L2NetworkInventory l2Network, String hostUuid) {
        return l2Network.getPhysicalInterface();
    }

    protected String makeBridgeName(String l2Uuid, int vlan, String hostUuid) {
        String bridgeName = L2NetworkHostUtils.getBridgeNameFromL2NetworkHostRef(l2Uuid, hostUuid);
        if (bridgeName != null) {
            return bridgeName;
        }

        if (vlan != 0) {
            return KVMHostUtils.getNormalizedBridgeName(l2Uuid, "br_%s_" + vlan);
        } else {
            return KVMHostUtils.getNormalizedBridgeName(l2Uuid, "br_%s");
        }
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final KVMAgentCommands.CreateVlanBridgeCmd cmd = new KVMAgentCommands.CreateVlanBridgeCmd();

        cmd.setPhysicalInterfaceName(getInterfaceName(l2Network, hostUuid));
        cmd.setBridgeName(makeBridgeName(l2Network.getUuid(), l2Network.getVirtualNetworkId(), hostUuid));
        cmd.setL2NetworkUuid(l2Network.getUuid());
        cmd.setDisableIptables(NetworkGlobalProperty.BRIDGE_DISABLE_IPTABLES);
        cmd.setMtu(new MtuGetter().getL2Mtu(l2Network));
        cmd.setVlan(l2Network.getVirtualNetworkId());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH);
        msg.setNoStatusCheck(noStatusCheck);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.CreateBridgeResponse rsp = hreply.toResponse(KVMAgentCommands.CreateBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr(
                            "failed to create bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s], because %s", cmd
                                    .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format(
                        "successfully realize bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid);
                if (l2Network.getVirtualNetworkId() == 0) {
                    String type = NetworkInterfaceType.bridgeSlave.toString();
                    SQL.New(HostNetworkInterfaceVO.class)
                            .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                            .eq(HostNetworkInterfaceVO_.interfaceName, getInterfaceName(l2Network, hostUuid))
                            .set(HostNetworkInterfaceVO_.interfaceType, type)
                            .update();
                    info = String.format("%s, try to set interface[%s] type to [%s] if it's physical interface",
                            info, getInterfaceName(l2Network, hostUuid), type);
                }
                logger.debug(info);

                if (!KVMSystemTags.L2_BRIDGE_NAME.hasTag(l2Network.getUuid())) {
                    SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(l2Network.getUuid());
                    creator.inherent = true;
                    creator.ignoreIfExisting = true;
                    creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, cmd.getBridgeName())));
                    creator.create();
                }

                completion.success();
            }
        });
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    public void check(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        final KVMAgentCommands.CheckBridgeCmd cmd = new KVMAgentCommands.CheckBridgeCmd();
        cmd.setPhysicalInterfaceName(getInterfaceName(l2Network, hostUuid));
        cmd.setBridgeName(makeBridgeName(l2Network.getUuid(), l2Network.getVirtualNetworkId(), hostUuid));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setNoStatusCheck(noStatusCheck);
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.CheckBridgeResponse rsp = hreply.toResponse(KVMAgentCommands.CheckBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to check bridge[%s] for l2NoVlanNetwork[uuid:%s, name:%s] on kvm host[uuid: %s], %s",
                            cmd.getBridgeName(), l2Network.getUuid(), l2Network.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked bridge[%s] for l2NoVlanNetwork[uuid:%s, name:%s] on kvm host[uuid: %s]",
                        cmd.getBridgeName(), l2Network.getUuid(), l2Network.getName(), hostUuid);
                logger.debug(info);
                completion.success();
            }
        });
    }

    @Override
    public void check(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        check(l2Network, hostUuid, false, completion);
    }

    @Override
    public void update(final L2NetworkInventory oldL2Network, final L2NetworkInventory newL2Network,
                       final String hostUuid, final Completion completion) {
        final KVMAgentCommands.UpdateBridgeCmd cmd = new KVMAgentCommands.UpdateBridgeCmd();

        cmd.setPhysicalInterfaceName(getInterfaceName(newL2Network, hostUuid));
        cmd.setBridgeName(makeBridgeName(newL2Network.getUuid(), newL2Network.getVirtualNetworkId(), hostUuid));
        cmd.setOldVirtualNetworkId(oldL2Network.getVirtualNetworkId());
        cmd.setNewVirtualNetworkId(newL2Network.getVirtualNetworkId());
        cmd.setL2NetworkUuid(newL2Network.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setNoStatusCheck(false);
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(KVMConstant.KVM_UPDATE_L2VLAN_NETWORK_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.UpdateL2NetworkResponse rsp = hreply.toResponse(KVMAgentCommands.UpdateL2NetworkResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to update bridge[%s] for l2Network[uuid:%s, name:%s] on kvm host[uuid: %s], %s",
                            cmd.getBridgeName(), newL2Network.getUuid(), newL2Network.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully update bridge[%s] for l2Network[uuid:%s, name:%s] on kvm host[uuid: %s]",
                        cmd.getBridgeName(), newL2Network.getUuid(), newL2Network.getName(), hostUuid);
                logger.debug(info);
                completion.success();
            }
        });
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public L2ProviderType getL2ProviderType() {
        return l2ProviderType;
    }

    @Override
    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        KVMAgentCommands.DeleteVlanBridgeCmd cmd = new KVMAgentCommands.DeleteVlanBridgeCmd();
        cmd.setPhysicalInterfaceName(getInterfaceName(l2Network, hostUuid));
        String bridgeName = makeBridgeName(l2Network.getUuid(), l2Network.getVirtualNetworkId(), hostUuid);
        if (L2NetworkHostUtils.isBridgeDeletionSkippedOnHost(l2Network.getUuid(), hostUuid)) {
            logger.debug(String.format("skip deleting bridge[%s] on host[uuid:%s]", bridgeName, hostUuid));
            completion.success();
            return;
        }
        cmd.setBridgeName(bridgeName);
        cmd.setL2NetworkUuid(l2Network.getUuid());
        cmd.setVlan(l2Network.getVirtualNetworkId());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(KVMConstant.KVM_DELETE_L2VLAN_NETWORK_PATH);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KVMAgentCommands.DeleteBridgeResponse rsp = hreply.toResponse(KVMAgentCommands.DeleteBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr(
                            "failed to delete bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s], because %s", cmd
                                    .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String message = String.format("successfully delete bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s]", cmd
                        .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid);
                if (l2Network.getVirtualNetworkId() == 0) {
                    String type = NetworkInterfaceType.noMaster.toString();
                    SQL.New(HostNetworkInterfaceVO.class)
                            .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                            .eq(HostNetworkInterfaceVO_.interfaceName, getInterfaceName(l2Network, hostUuid))
                            .set(HostNetworkInterfaceVO_.interfaceType, type)
                            .update();
                    message = String.format("%s, try to set interface[%s] type to [%s] if it's physical interface",
                            message, getInterfaceName(l2Network, hostUuid), type);
                }
                logger.debug(message);

                completion.success();
            }
        });
    }
}
