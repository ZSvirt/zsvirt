package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.vm.*;
import org.zstack.network.securitygroup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;


public class SimulatorSecurityGroupBackend implements
		SecurityGroupHypervisorBackend, VmInstanceStateChangeNotifyPoint {
	private static CLogger logger = Utils.getLogger(SimulatorSecurityGroupBackend.class);
	
	private Map<String, Set<SecurityGroupRuleTO>> rules = new HashMap<String, Set<SecurityGroupRuleTO>>();
	
	public volatile boolean securityGroupSuccess = true;
	
	@Autowired
	private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

	@Override
	public void applyRules(HostRuleTO hto, Completion complete) {
		logger.debug(String.format("apply security rules to simulator host[uuid:%s], ipv4:\n%s\nipv6:\n%s", hto.getHostUuid(), JSONObjectUtil.toJsonString(hto.getRules()), JSONObjectUtil.toJsonString(hto.getIp6Rules())));
		if (!securityGroupSuccess) {
		    ErrorCode errorCode = operr("on purpose");
		    complete.fail(errorCode);
		    return;
		}
		
		complete.success();
	}

    @Override
    public void checkDefaultRules(String hostUuid, Completion completion) {
        completion.success();
    }

	@Override
	public void updateGroupMembers(SecurityGroupMembersTO gto, String hostUuid, Completion completion) {

	}

    @Override
    public void cleanUpUnusedRuleOnHost(String hostUuid, Completion completion) {
        rules.put(hostUuid, new HashSet<>());
        completion.success();
    }

    public Set<SecurityGroupRuleTO> getRulesOnHost(String hostUuid) {
		return rules.get(hostUuid);
	}
	
	public SecurityGroupRuleTO getRulesOnHost(String hostUuid, String vmNicName) {
        Set<SecurityGroupRuleTO> tos = getRulesOnHost(hostUuid);
        if (tos == null) {
            return null;
        }

	    // for (SecurityGroupRuleTO to : tos) {
	    //     if (to.getVmNicInternalName().equals(vmNicName)) {
	    //         return to;
	    //     }
	    // }
	    
	    return null;
	}
	
	public List<RuleTO> getRulesByNicName(String nicName) {
		// for (Set<SecurityGroupRuleTO> rs : rules.values()) {
		// 	for (SecurityGroupRuleTO sto : rs) {
		// 		if (sto.getVmNicInternalName().equals(nicName)) {
		// 			return sto.getRules();
		// 		}
		// 	}
		// }
		
		return null;
	}

	@Override
	public HypervisorType getSecurityGroupBackendHypervisorType() {
		return HypervisorType.valueOf(SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE);
	}

    @Override
    public void notifyVmInstanceStateChange(VmInstanceInventory inv, VmInstanceState previousState, VmInstanceState currentState) {
        if (previousState == VmInstanceState.Unknown && currentState == VmInstanceState.Running) {
            RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
            msg.setVmInstanceUuid(inv.getUuid());
            msg.setServiceId(bus.makeLocalServiceId(SecurityGroupConstant.SERVICE_ID));
            bus.send(msg);
        }
    }

    @Override
    public HypervisorType getSupportedHypervisorTypeForVmInstanceStateChangeNotifyPoint() {
        return HypervisorType.valueOf(SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE);
    }

}
