package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIChangeSecurityGroupRuleStateEvent

doc {
	title "ChangeSecurityGroupRuleState"

	category "securityGroup"

	desc """更改安全组规则状态"""

	rest {
		request {
			url "PUT /v1/security-groups/{securityGroupUuid}/rules/state/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeSecurityGroupRuleStateMsg.class

			desc """更改安全组规则状态"""

			params {

				column {
					name "securityGroupUuid"
					enclosedIn "changeSecurityGroupRuleState"
					desc "安全组的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "ruleUuids"
					enclosedIn "changeSecurityGroupRuleState"
					desc "规则的UUID列表"
					location "body"
					type "List"
					optional false
					since "3.17.21"
				}
				column {
					name "state"
					enclosedIn "changeSecurityGroupRuleState"
					desc "规则的状态"
					location "body"
					type "String"
					optional false
					since "3.17.21"
					values ("Enabled","Disabled")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
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
			clz APIChangeSecurityGroupRuleStateEvent.class
		}
	}
}