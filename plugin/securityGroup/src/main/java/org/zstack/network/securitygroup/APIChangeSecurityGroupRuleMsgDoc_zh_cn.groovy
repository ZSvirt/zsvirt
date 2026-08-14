package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIChangeSecurityGroupRuleEvent

doc {
	title "ChangeSecurityGroupRule"

	category "securityGroup"

	desc """更改安全组规则"""

	rest {
		request {
			url "PUT /v1/security-groups/rules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeSecurityGroupRuleMsg.class

			desc """更改安全组规则"""

			params {

				column {
					name "uuid"
					enclosedIn "changeSecurityGroupRule"
					desc "安全组规则的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "description"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的描述"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "remoteSecurityGroupUuid"
					enclosedIn "changeSecurityGroupRule"
					desc "应用组间策略的远端安全组UUID"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "action"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的默认动作"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("DROP","ACCEPT")
				}
				column {
					name "state"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的状态"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("Enabled","Disabled")
				}
				column {
					name "priority"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的优先级"
					location "body"
					type "Integer"
					optional true
					since "3.17.21"
				}
				column {
					name "protocol"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的协议类型"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("ALL","TCP","UDP","ICMP")
				}
				column {
					name "srcIpRange"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的源方向IP范围"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "dstIpRange"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的目的方向IP范围"
					location "body"
					type "String"
					optional true
					since "3.17.21"
				}
				column {
					name "dstPortRange"
					enclosedIn "changeSecurityGroupRule"
					desc "规则的目的方向端口范围"
					location "body"
					type "String"
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
			clz APIChangeSecurityGroupRuleEvent.class
		}
	}
}