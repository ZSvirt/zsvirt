package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.Map.Entry;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmSetFirewallFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(ApplianceVmSetFirewallFlow.class);

    /* no need to rollback, if any failure happen the vm is going to destroy */

    @Autowired
    private ApplianceVmFacadeImpl apvmf;

    private void setFirewall(final String apvmUuid, final Iterator<Entry<String, List<ApplianceVmFirewallRuleInventory>>> it, final FlowTrigger trigger) {
        if (!it.hasNext()) {
            trigger.next();
            return;
        }

        final Entry<String, List<ApplianceVmFirewallRuleInventory>> e = it.next();
        apvmf.openFirewallInBootstrap(apvmUuid, e.getKey(), e.getValue(), new Completion(trigger) {
            @Override
            public void success() {
                logger.debug(String.format("set firewall for l3Network[uuid:%s] on appliance vm[uuid:%s]", e.getKey(), apvmUuid));
                setFirewall(apvmUuid, it, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    private Map<String, List<ApplianceVmFirewallRuleInventory>> normalize(List<ApplianceVmFirewallRuleInventory> rules) {
        Map<String, List<ApplianceVmFirewallRuleInventory>> networkFirewallRules = new HashMap<String, List<ApplianceVmFirewallRuleInventory>>();
        for (ApplianceVmFirewallRuleInventory rule : rules) {
            List<ApplianceVmFirewallRuleInventory> rs = networkFirewallRules.computeIfAbsent(rule.getL3NetworkUuid(), k -> new ArrayList<ApplianceVmFirewallRuleInventory>());
            rs.add(rule);
        }
        return networkFirewallRules;
    }

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<ApplianceVmFirewallRuleInventory> rules = (List<ApplianceVmFirewallRuleInventory>) data.get(ApplianceVmConstant.Params.applianceVmFirewallRules.toString());
        if (rules == null || rules.isEmpty()) {
            trigger.next();
            return;
        }

        boolean isReconnect = Boolean.parseBoolean((String) data.get(Params.isReconnect.toString()));
        Map<String, List<ApplianceVmFirewallRuleInventory>> networkFirewallRules = normalize(rules);
        if (isReconnect) {
            setFirewall((String) data.get(Params.applianceVmUuid.toString()), networkFirewallRules.entrySet().iterator(), trigger);
        } else {
            setFirewall(spec.getVmInventory().getUuid(), networkFirewallRules.entrySet().iterator(), trigger);
        }
    }
}
