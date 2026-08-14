package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIChangeVmNicSecurityPolicyEvent

doc {
	title "ChangeVmNicSecurityPolicy"

	category "securityGroup"

	desc """更改网卡的默认流量策略"""

	rest {
		request {
			url "PUT /v1/security-groups/nics/{vmNicUuid}/security-policy/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVmNicSecurityPolicyMsg.class

			desc """更改网卡的安全策略"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn "changeVmNicSecurityPolicy"
					desc "网卡的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "ingressPolicy"
					enclosedIn "changeVmNicSecurityPolicy"
					desc "网卡入方向安全策略"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("DENY","ALLOW")
				}
				column {
					name "egressPolicy"
					enclosedIn "changeVmNicSecurityPolicy"
					desc "网卡出方向安全策略"
					location "body"
					type "String"
					optional true
					since "3.17.21"
					values ("DENY","ALLOW")
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
			clz APIChangeVmNicSecurityPolicyEvent.class
		}
	}
}