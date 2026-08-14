package org.zstack.simulator2.agents

import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.simulator2.Simulator

/**
 * Created by xing5 on 2017/9/19.
 */
class SecurityGroupAgent extends Agent {
    SecurityGroupAgent(Simulator simulator) {
        super(simulator)
    }

    @Override
    void setupAgentHandler() {
        handle(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) {
            return new KVMAgentCommands.ApplySecurityGroupRuleResponse()
        }

        handle(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.RefreshAllRulesOnHostResponse()
        }

        handle(KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.CleanupUnusedRulesOnHostResponse()
        }

        handle(KVMSecurityGroupBackend.SECURITY_GROUP_UPDATE_GROUP_MEMBER){
            return new KVMAgentCommands.UpdateGroupMemberResponse()
        }
    }
}
