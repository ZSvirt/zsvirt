package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmFacade;
import org.zstack.appliancevm.ApplianceVmFirewallRuleInventory;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingRemoveVirtualRouterFirewallFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(PortForwardingRemoveVirtualRouterFirewallFlow.class);

    @Autowired
    private ApplianceVmFacade apvmf;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final PortForwardingRuleTO to = (PortForwardingRuleTO) data.get(VirtualRouterConstant.VR_PORT_FORWARDING_RULE);
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.VR_RESULT_VM);
        final String targetL3 = (String) data.get(VirtualRouterConstant.VR_VIP_L3NETWORK);

        final ApplianceVmFirewallRuleInventory rule = new ApplianceVmFirewallRuleInventory();
        rule.setProtocol(to.getProtocolType().toLowerCase());
        rule.setDestIp(to.getVipIp());
        rule.setEndPort(to.getPrivatePortEnd());
        rule.setStartPort(to.getPrivatePortStart());
        rule.setAllowCidr(to.getAllowedCidr());
        apvmf.removeFirewall(vr.getUuid(), targetL3, Arrays.asList(rule), new Completion(trigger) {
            @Override
            public void success() {
                logger.debug(String.format("successfully removed firewall on virtual route[uuid:%s, name:%s] for port forwarding, firewall rule: %s",
                        vr.getUuid(), vr.getName(), JSONObjectUtil.toJsonString(rule)));
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
