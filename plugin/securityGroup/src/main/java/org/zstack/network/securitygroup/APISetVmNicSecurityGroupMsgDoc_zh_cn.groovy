package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APISetVmNicSecurityGroupEvent

doc {
	title "SetVmNicSecurityGroup"

	category "securityGroup"

	desc """设置网卡的安全组"""

	rest {
		request {
			url "PUT /v1/security-groups/nics/{vmNicUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmNicSecurityGroupMsg.class

			desc """设置网卡的安全组"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn "setVmNicSecurityGroup"
					desc "网卡的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "refs"
					enclosedIn "setVmNicSecurityGroup"
					desc "网卡挂载的安全组"
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
			clz APISetVmNicSecurityGroupEvent.class
		}
	}
}