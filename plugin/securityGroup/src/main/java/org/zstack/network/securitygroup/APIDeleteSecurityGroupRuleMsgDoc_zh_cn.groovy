package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIDeleteSecurityGroupRuleEvent

doc {
	title "DeleteSecurityGroupRule"

	category "securityGroup"

	desc """删除安全组规则,这个命令是异步执行的, 在它返回后可能规则仍然没有对所有的主机上生效"""

	rest {
		request {
			url "DELETE /v1/security-groups/rules"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteSecurityGroupRuleMsg.class

			desc """删除安全组规则,这个命令是异步执行的, 在它返回后可能规则仍然没有对所有的主机上生效"""

			params {

				column {
					name "ruleUuids"
					enclosedIn ""
					desc "安全组规则的uuid列表"
					location "query"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteSecurityGroupRuleEvent.class
		}
	}
}