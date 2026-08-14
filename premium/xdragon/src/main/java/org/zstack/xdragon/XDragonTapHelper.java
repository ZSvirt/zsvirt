package org.zstack.xdragon;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.flat.BridgeNameFinder;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class XDragonTapHelper {
    private static final CLogger logger = Utils.getLogger(XDragonTapHelper.class);

    private static final String defaultBridgeType = "iohub";
    static final String bridgeTypeMocbr = "mocbr";

    @Autowired
    private CloudBus bus;

    public void addtap(String hostUuid, String br, Completion completion) {
        if (XDragonGlobalProperty.tapName == null || XDragonGlobalProperty.tapName.isEmpty()) {
            completion.success();
            return;
        }

        logger.info(String.format("adding %s to %s", XDragonGlobalProperty.tapName, br));

        KVMAgentCommands.AddInterfaceToBridgeCmd cmd = new KVMAgentCommands.AddInterfaceToBridgeCmd();
        cmd.setBridgeName(br);
        cmd.setPhysicalInterfaceName(XDragonGlobalProperty.tapName);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setNoStatusCheck(false);
        msg.setPath(KVMConstant.KVM_ADD_INTERFACE_TO_BRIDGE_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                completion.success();
            }
        });
    }

    public static String getBridgeMode(String clusterUuid) {
        String brtype = XDragonSystemTags.CLUSTER_BRIDGE_MODE.getTokenByResourceUuid(
                clusterUuid,
                XDragonSystemTags.CLUSTER_BRIDGE_MODE_TOKEN);
        return brtype == null ? defaultBridgeType : brtype;
    }

    static String getBridgeNameByL3(String l3uuid) {
        return new BridgeNameFinder().findByL3Uuid(l3uuid, false);
    }

    static String getBridgeNameByL2(String l2uuid) {
        return KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2uuid, L2NetworkVO.class, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
    }
}
