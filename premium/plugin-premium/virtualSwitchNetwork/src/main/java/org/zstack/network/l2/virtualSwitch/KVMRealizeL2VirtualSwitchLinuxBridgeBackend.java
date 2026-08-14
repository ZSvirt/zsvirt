package org.zstack.network.l2.virtualSwitch;

import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.operr;

public class KVMRealizeL2VirtualSwitchLinuxBridgeBackend extends KVMRealizeL2NoVlanLinuxBridgeBackend {
    private static final CLogger logger = Utils.getLogger(KVMRealizeL2VirtualSwitchLinuxBridgeBackend.class);

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE);
    }

    @Override
    public void realize(final L2NetworkInventory l2Network, final String hostUuid, boolean noStatusCheck, final Completion completion) {
        completion.success();
    }

    @Override
    public void update(final L2NetworkInventory oldL2Network, final L2NetworkInventory newL2Network,
                       final String hostUuid, final Completion completion) {
        // oldL2Network is only used to record the original interface name here
        final MevocoKVMAgentCommands.BatchUpdateBridgeCmd cmd = new MevocoKVMAgentCommands.BatchUpdateBridgeCmd();

        UplinkGroupVO ug = VirtualSwitchUtils.getUplinkGroup(newL2Network.getUuid(), hostUuid);
        if (UplinkGroupType.Bonding.equals(ug.getType())) {
            HostNetworkBondingVO bondingVO = Q.New(HostNetworkBondingVO.class)
                    .eq(HostNetworkBondingVO_.bondingName, ug.getInterfaceName())
                    .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                    .find();

            if (bondingVO == null) {
                completion.fail(operr("bonding[%s] is not found on host[uuid:%s] for virtual switch[uuid:%s]",
                        ug.getInterfaceName(), hostUuid, newL2Network.getUuid()));
                return;
            }

            HostNetworkBondingInventory bondingInv = HostNetworkBondingInventory.valueOf(bondingVO);
            MevocoKVMAgentCommands.CreateBondingCmd createCmd = new MevocoKVMAgentCommands.CreateBondingCmd();
            createCmd.setBondName(bondingInv.getBondingName());
            createCmd.setSlaves(bondingInv.getSlaves());
            createCmd.setType(bondingInv.getType());
            createCmd.setMode(bondingInv.getMode());
            createCmd.setXmitHashPolicy(bondingInv.getXmitHashPolicy());
            cmd.setNewBonding(createCmd);
        } else {
            cmd.setNewPhysicalInterface(ug.getInterfaceName());
        }

        if (!Objects.equals(oldL2Network.getPhysicalInterface(), newL2Network.getPhysicalInterface())) {
            cmd.setOldPhysicalInterface(oldL2Network.getPhysicalInterface());
        } else {
            cmd.setOldBondingName(oldL2Network.getPhysicalInterface());
        }

        List<Tuple> tuples = Q.New(L2PortGroupNetworkVO.class)
                .select(L2PortGroupNetworkVO_.uuid, L2PortGroupNetworkVO_.vlanId)
                .eq(L2PortGroupNetworkVO_.vSwitchUuid, newL2Network.getUuid())
                .listTuple();

        List<MevocoKVMAgentCommands.BridgeParam> bridgeParams = new ArrayList<>();
        for (Tuple tuple : tuples) {
            String l2Uuid = tuple.get(0, String.class);
            int vlanId = tuple.get(1, Integer.class);

            MevocoKVMAgentCommands.BridgeParam param = new MevocoKVMAgentCommands.BridgeParam();
            param.setBridgeName(makeBridgeName(l2Uuid, vlanId, hostUuid));
            param.setVlanId(vlanId);
            param.setL2NetworkUuid(l2Uuid);
            bridgeParams.add(param);
        }
        cmd.setBridgeParams(bridgeParams);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setNoStatusCheck(false);
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(MevocoKVMConstant.BATCH_UPDATE_BRIDGE_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply hreply = reply.castReply();
                MevocoKVMAgentCommands.BatchUpdateBridgeRsp rsp = hreply.toResponse(MevocoKVMAgentCommands.BatchUpdateBridgeRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("failed to update vlan bridge for virtual switch[uuid:%s, name:%s] on kvm host[uuid:%s], %s",
                            newL2Network.getUuid(), newL2Network.getName(), hostUuid, rsp.getError()));
                    return;
                }

                logger.debug(String.format("successfully update vlan bridge for virtual switch[uuid:%s, name:%s] on kvm host[uuid:%s]",
                        newL2Network.getUuid(), newL2Network.getName(), hostUuid));
                completion.success();
            }
        });
    }

    @Override
    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }
}
