package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIUpdateSecurityGroupRulePriorityEvent

doc {
	title "UpdateSecurityGroupRulePriority"

	category "securityGroup"

	desc """更新安全组规则的优先级"""

	rest {
		request {
			url "PUT /v1/security-groups/{securityGroupUuid}/rules/priority/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSecurityGroupRulePriorityMsg.class

			desc """更新安全组规则的优先级"""

			params {

				column {
					name "securityGroupUuid"
					enclosedIn "updateSecurityGroupRulePriority"
					desc "安全组的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "type"
					enclosedIn "updateSecurityGroupRulePriority"
					desc "规则的类型"
					location "body"
					type "String"
					optional false
					since "3.17.21"
					values ("Ingress","Egress")
				}
				column {
					name "rules"
					enclosedIn "updateSecurityGroupRulePriority"
					desc "规则的优先级"
					location "body"
					type "List"
					optional false
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIUpdateSecurityGroupRulePriorityEvent.class
		}
	}
}