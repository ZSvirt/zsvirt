package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIAddSecurityGroupRuleEvent

doc {
	title "AddSecurityGroupRule"

	category "securityGroup"

	desc """用户可以使用AddSecurityGroupRule添加规则到安全组"""

	rest {
		request {
			url "POST /v1/security-groups/{securityGroupUuid}/rules"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSecurityGroupRuleMsg.class

			desc """用户可以使用AddSecurityGroupRule添加规则到安全组"""

			params {

				column {
					name "securityGroupUuid"
					enclosedIn "params"
					desc "安全组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "rules"
					enclosedIn "params"
					desc "安全组中的规则"
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "remoteSecurityGroupUuids"
					enclosedIn "params"
					desc "应用组间策略的远端安全组UUID"
					location "body"
					type "List"
					optional true
					since "2.1"
				}
				column {
					name "priority"
					enclosedIn "params"
					desc "规则优先级"
					location "body"
					type "Integer"
					optional true
					since "3.17.21"
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
			clz APIAddSecurityGroupRuleEvent.class
		}
	}
}