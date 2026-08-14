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
import org.zstack.network.service.vip.VipInventory;
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
public class PortForwardingOpenVirtualRouterFirewallFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(PortForwardingOpenVirtualRouterFirewallFlow.class);

    @Autowired
    private ApplianceVmFacade apvmf;

    private static final String SUCCESS = "PortForwarding.OpenVirtualRouterFirewall";

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final PortForwardingRuleTO to = (PortForwardingRuleTO) data.get(VirtualRouterConstant.VR_PORT_FORWARDING_RULE);
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.VR_RESULT_VM);
        final VipInventory vip = (VipInventory) data.get(VirtualRouterConstant.VR_VIP);

        final ApplianceVmFirewallRuleInventory rule = new ApplianceVmFirewallRuleInventory();
        rule.setDestIp(vip.getIp());
        rule.setProtocol(to.getProtocolType().toLowerCase());
        rule.setEndPort(to.getPrivatePortEnd());
        rule.setStartPort(to.getPrivatePortStart());
        rule.setAllowCidr(to.getAllowedCidr());
        apvmf.openFirewall(vr.getUuid(), vip.getL3NetworkUuid(), Arrays.asList(rule), new Completion(trigger) {
            @Override
            public void success() {
                logger.debug(String.format("successfully open firewall on virtual route[uuid:%s, name:%s] for port forwarding, firewall rule: %s",
                        vr.getUuid(), vr.getName(), JSONObjectUtil.toJsonString(rule)));
                data.put(SUCCESS, rule);
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
        final ApplianceVmFirewallRuleInventory rule = (ApplianceVmFirewallRuleInventory) data.get(SUCCESS);
        if (rule == null) {
            trigger.rollback();
            return;
        }

        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.VR_RESULT_VM);
        final VipInventory vip = (VipInventory) data.get(VirtualRouterConstant.VR_VIP);
        apvmf.removeFirewall(vr.getUuid(), vip.getL3NetworkUuid(), Arrays.asList(rule), new Completion(trigger) {
            @Override
            public void success() {
                logger.debug(String.format("successfully rollback firewall on virtual route[uuid:%s, name:%s] for port forwarding, firewall rule: %s",
                        vr.getUuid(), vr.getName(), JSONObjectUtil.toJsonString(rule)));
                trigger.rollback();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to rollback firewall on virtual route[uuid:%s, name:%s] for port forwarding, firewall rule: %s",
                        vr.getUuid(), vr.getName(), JSONObjectUtil.toJsonString(rule)));
                trigger.rollback();
            }
        });
    }
}
