package org.zstack.xdragon;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

public class XDragonHostConnectExtensionPoint implements KVMHostConnectExtensionPoint {
    private static final CLogger logger = Utils.getLogger(XDragonHostConnectExtensionPoint.class);

    @Autowired
    private CloudBus bus;

    public static String INIT_HOST_MOC_PATH = "/host/initmoc";

    public static class InitMocCmd extends KVMAgentCommands.AgentCommand {
        private String mode;
        private String masterVethName;
        private String bridgeName;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getMasterVethName() {
            return masterVethName;
        }

        public void setMasterVethName(String masterVethName) {
            this.masterVethName = masterVethName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        if (!XDragonConstant.HYPERVISOR_TYPE.equals(context.getInventory().getHypervisorType())) {
            return new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    trigger.next();
                }
            };
        }

        return new NoRollbackFlow() {
            String __name__ = "prepare-bridges";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                String hostUuid = context.getInventory().getUuid();
                String clusterUuid = context.getInventory().getClusterUuid();
                if (clusterUuid == null) {
                    logger.info("no cluster found for host: " + hostUuid);
                    trigger.next();
                    return;
                }

                InitMocCmd cmd = new InitMocCmd();
                cmd.setMode(XDragonTapHelper.getBridgeMode(clusterUuid));
                cmd.setMasterVethName(XDragonGlobalProperty.masterVethName);

                String l2uuid = Q.New(L2NetworkClusterRefVO.class)
                        .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid)
                        .select(L2NetworkClusterRefVO_.l2NetworkUuid)
                        .limit(1)
                        .findValue();
                cmd.setBridgeName(XDragonTapHelper.getBridgeNameByL2(l2uuid));

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(hostUuid);
                msg.setCommand(cmd);
                msg.setNoStatusCheck(true);
                msg.setPath(INIT_HOST_MOC_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn("failed to prepare bridge: " + reply.getError().getDetails());
                        }

                        trigger.next();
                    }
                });
            }
        };
    }

}
