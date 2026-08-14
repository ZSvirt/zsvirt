package org.zstack.macvlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class KVMRealizeL2VlanMacVlanBackend implements L2NetworkRealizationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMRealizeL2VlanMacVlanBackend.class);
    private static final L2ProviderType l2ProviderType = new L2ProviderType(KVMConstant.L2_PROVIDER_TYPE_MACVLAN);

    @Autowired
    private CloudBus bus;

    private static String makeBridgeName(String hostNetworkInterfaceName, int vlan) {
        return String.format("%s.%s", hostNetworkInterfaceName, vlan);
    }

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        final L2VlanNetworkInventory l2vlan = (L2VlanNetworkInventory) l2Network;
        final KvmAgentL2NetworkMacVlanCommands.CreateVlanBridgeCmd cmd = new KvmAgentL2NetworkMacVlanCommands.CreateVlanBridgeCmd();
        cmd.setVlan(l2vlan.getVlan());
        cmd.setPhysicalInterfaceName(l2vlan.getPhysicalInterface());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(L2NetworkMacVlanConstant.KVM_REALIZE_L2VLAN_NETWORK_MACVLAN_PATH);
        msg.setNoStatusCheck(noStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KvmAgentL2NetworkMacVlanCommands.CreateVlanBridgeResponse rsp = hreply.toResponse(KvmAgentL2NetworkMacVlanCommands.CreateVlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to realize bridge for l2VlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                            l2vlan.getUuid(), l2vlan.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully realize bridge  for l2VlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s]",
                        l2vlan.getUuid(), l2vlan.getName(), hostUuid);
                logger.debug(info);

                SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(l2Network.getUuid());
                creator.inherent = true;
                creator.ignoreIfExisting = true;
                creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, makeBridgeName(cmd.getPhysicalInterfaceName(), cmd.getVlan()))));
                creator.create();

                completion.success();
            }
        });
    }

    @Override
    public void check(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        final L2VlanNetworkInventory l2vlan = (L2VlanNetworkInventory) l2Network;
        final KvmAgentL2NetworkMacVlanCommands.CheckVlanBridgeCmd cmd = new KvmAgentL2NetworkMacVlanCommands.CheckVlanBridgeCmd();
        cmd.setVlan(l2vlan.getVlan());
        cmd.setPhysicalInterfaceName(l2vlan.getPhysicalInterface());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(L2NetworkMacVlanConstant.KVM_CHECK_L2VLAN_NETWORK_MACVLAN_PATH);
        msg.setNoStatusCheck(false);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KvmAgentL2NetworkMacVlanCommands.CheckVlanBridgeResponse rsp = hreply.toResponse(KvmAgentL2NetworkMacVlanCommands.CheckVlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to check vlan interface[%s] for l2VlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                            cmd.getPhysicalInterfaceName() + "." + cmd.getVlan(), l2vlan.getUuid(), l2vlan.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked vlan interface[%s] for l2VlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s]",
                        cmd.getPhysicalInterfaceName() + "." + cmd.getVlan(), l2vlan.getUuid(), l2vlan.getName(), hostUuid);
                logger.debug(info);
                completion.success();
            }
        });
    }

    @Override
    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        final L2VlanNetworkInventory l2vlan = (L2VlanNetworkInventory) l2Network;
        final KvmAgentL2NetworkMacVlanCommands.DeleteVlanBridgeCmd cmd = new KvmAgentL2NetworkMacVlanCommands.DeleteVlanBridgeCmd();
        cmd.setVlan(l2vlan.getVlan());
        cmd.setPhysicalInterfaceName(l2vlan.getPhysicalInterface());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(L2NetworkMacVlanConstant.KVM_DELETE_L2VLAN_NETWORK_MACVLAN_PATH);
        msg.setNoStatusCheck(false);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                KvmAgentL2NetworkMacVlanCommands.DeleteVlanBridgeResponse rsp = hreply.toResponse(KvmAgentL2NetworkMacVlanCommands.DeleteVlanBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr("failed to check vlan interface[%s] for l2VlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                            cmd.getPhysicalInterfaceName() + "." + cmd.getVlan(), l2vlan.getUuid(), l2vlan.getName(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String info = String.format("successfully checked vlan interface[%s] for l2VlanNetwork[uuid:%s, name:%s] on kvm host[uuid:%s]",
                        cmd.getPhysicalInterfaceName() + "." + cmd.getVlan(), l2vlan.getUuid(), l2vlan.getName(), hostUuid);
                logger.debug(info);
                completion.success();
            }
        });
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public L2ProviderType getL2ProviderType() {
        return l2ProviderType;
    }
}
