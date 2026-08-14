package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.network.l3.NetworkGlobalProperty;
import org.zstack.network.service.MtuGetter;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class KVMRealizeL2NoVlanNetworkBackend implements L2NetworkRealizationExtensionPoint, KVMCompleteNicInformationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KVMRealizeL2NoVlanNetworkBackend.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private ThreadFacade thdf;

    private static String makeBridgeName(String l2Uuid) {
        return KVMHostUtils.getNormalizedBridgeName(l2Uuid, "br_%s");
    }

    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion, final String cmdPath) {
        final CreateBridgeCmd cmd = new CreateBridgeCmd();
        cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        cmd.setBridgeName(makeBridgeName(l2Network.getUuid()));
        cmd.setL2NetworkUuid(l2Network.getUuid());
        cmd.setDisableIptables(NetworkGlobalProperty.BRIDGE_DISABLE_IPTABLES);
        cmd.setMtu(new MtuGetter().getL2Mtu(l2Network));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(cmdPath);
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
                CreateBridgeResponse rsp = hreply.toResponse(CreateBridgeResponse.class);
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
                logger.debug(info);

                SystemTagCreator creator = KVMSystemTags.L2_BRIDGE_NAME.newSystemTagCreator(l2Network.getUuid());
                creator.inherent = true;
                creator.ignoreIfExisting = true;
                creator.setTagByTokens(map(e(KVMSystemTags.L2_BRIDGE_NAME_TOKEN, cmd.getBridgeName())));
                creator.create();

                completion.success();
            }
        });
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        if (l2Network.getvSwitchType().equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)) {
            realize(l2Network, hostUuid, noStatusCheck, completion, KVMConstant.KVM_REALIZE_OVSDPDK_NETWORK_PATH);
        } else {
            realize(l2Network, hostUuid, noStatusCheck, completion, KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH);
        }
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, final Completion completion) {
        realize(l2Network, hostUuid, false, completion);
    }

    public void check(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion, final String cmdPath) {
        final KVMAgentCommands.CheckBridgeCmd cmd = new KVMAgentCommands.CheckBridgeCmd();
        cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        cmd.setBridgeName(makeBridgeName(l2Network.getUuid()));

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setNoStatusCheck(noStatusCheck);
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(cmdPath);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                CheckBridgeResponse rsp = hreply.toResponse(CheckBridgeResponse.class);
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
        if (l2Network.getvSwitchType().equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)) {
            check(l2Network, hostUuid, false, completion, KVMConstant.KVM_CHECK_OVSDPDK_NETWORK_PATH);
        } else {
            check(l2Network, hostUuid, false, completion, KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH);
        }
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
    public L2NetworkType getL2NetworkTypeVmNicOn() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    }

    @Override
    public NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic) {
        NicTO to = KVMAgentCommands.NicTO.fromVmNicInventory(nic);
        to.setBridgeName(makeBridgeName(l2Network.getUuid()));
        to.setPhysicalInterface(l2Network.getPhysicalInterface());
        to.setMtu(new MtuGetter().getMtu(l3Network.getUuid()));

        return to;
    }

    @Override
    public String getBridgeName(L2NetworkInventory l2Network) {
        return makeBridgeName(l2Network.getUuid());
    }


    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion, final String cmdPath) {
        DeleteBridgeCmd cmd = new DeleteBridgeCmd();
        cmd.setPhysicalInterfaceName(l2Network.getPhysicalInterface());
        cmd.setBridgeName(makeBridgeName(l2Network.getUuid()));
        cmd.setL2NetworkUuid(l2Network.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setPath(cmdPath);
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
                DeleteBridgeResponse rsp = hreply.toResponse(DeleteBridgeResponse.class);
                if (!rsp.isSuccess()) {
                    ErrorCode err = operr(
                            "failed to delete bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s], because %s", cmd
                                    .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid, rsp.getError());
                    completion.fail(err);
                    return;
                }

                String message = String.format(
                        "successfully delete bridge[%s] for l2Network[uuid:%s, type:%s] on kvm host[uuid:%s]", cmd
                                .getBridgeName(), l2Network.getUuid(), l2Network.getType(), hostUuid);
                logger.debug(message);

                completion.success();
            }
        });
    }

    @Override
    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        if (l2Network.getvSwitchType().equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)) {
            deleteOvsBridge(l2Network, hostUuid, completion);
        } else {
            delete(l2Network, hostUuid, completion, KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH);
        }
    }

    private void deleteOvsBridge(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public void run(SyncTaskChain chain) {
                // vlan bridges and novlan bridge use the same bridge name
                // in ovs, so before delete l2 network we should check if
                // the PhysicalInterface is using by another l2 network.
                List<L2NetworkVO> l2NetworkVOs = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.physicalInterface, l2Network.getPhysicalInterface())
                        .notEq(L2NetworkVO_.uuid, l2Network.getUuid()).list();

                if (l2NetworkVOs.isEmpty()) {
                    delete(l2Network, hostUuid, new Completion(completion) {
                        @Override
                        public void success() {
                            completion.success();
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                            chain.next();
                        }
                    }, KVMConstant.KVM_DELETE_OVSDPDK_NETWORK_PATH);
                    return;
                }
                boolean noNeedDelete = false;
                String clusterUuid = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.clusterUuid).findValue();
                for (L2NetworkVO l2 : l2NetworkVOs) {
                    boolean anotherl2AttachCluster = l2.getAttachedClusterRefs().stream().anyMatch(ref -> ref.getClusterUuid().equals(clusterUuid));
                    if (anotherl2AttachCluster) {
                        noNeedDelete = true;
                        break;
                    }
                }
                if (noNeedDelete) {
                    completion.success();
                    chain.next();
                    return;
                }
                delete(l2Network, hostUuid, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                }, KVMConstant.KVM_DELETE_OVSDPDK_NETWORK_PATH);
            }

            @Override
            public String getSyncSignature() {
                return String.format("delete-ovsbridge-interface-%s-in-host-%s", l2Network.getPhysicalInterface(), hostUuid);
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
}
