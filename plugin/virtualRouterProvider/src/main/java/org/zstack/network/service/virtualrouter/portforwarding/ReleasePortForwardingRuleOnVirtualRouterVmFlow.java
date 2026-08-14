package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.FirewallCanonicalEvents;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RevokePortForwardingRuleRsp;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.network.service.virtualrouter.VirtualRouterBootstrapIsoVO_.virtualRouterUuid;

import java.util.Arrays;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ReleasePortForwardingRuleOnVirtualRouterVmFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(ReleasePortForwardingRuleOnVirtualRouterVmFlow.class);

    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    @Qualifier("VirtualRouterPortForwardingBackend")
    protected VirtualRouterPortForwardingBackend backend;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private EventFacade evtf;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final PortForwardingRuleTO to = (PortForwardingRuleTO) data.get(VirtualRouterConstant.VR_PORT_FORWARDING_RULE);
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.VR_RESULT_VM);

        VirtualRouterCommands.RevokePortForwardingRuleCmd cmd = new VirtualRouterCommands.RevokePortForwardingRuleCmd();
        cmd.setRules(Arrays.asList(to));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    String err = String.format("failed to revoke port forwarding rules %s, because %s", JSONObjectUtil.toJsonString(to), reply.getError());
                    logger.warn(err);
                    chain.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                RevokePortForwardingRuleRsp ret = re.toResponse(RevokePortForwardingRuleRsp.class);
                if (ret.isSuccess()) {
                    String info = String.format("successfully revoke port forwarding rules: %s", JSONObjectUtil.toJsonString(to));
                    logger.debug(info);
                    chain.next();
                    FirewallCanonicalEvents.FirewallRuleChangedData data = new FirewallCanonicalEvents.FirewallRuleChangedData();
                    data.setVirtualRouterUuid(vr.getUuid());
                    evtf.fire(FirewallCanonicalEvents.FIREWALL_RULE_CHANGED_PATH, data);
                } else {
                    ErrorCode err = operr("failed to revoke port forwarding rules %s, because %s", JSONObjectUtil.toJsonString(to), ret.getError());
                    chain.fail(err);
                }
            }
        });
    }
}
