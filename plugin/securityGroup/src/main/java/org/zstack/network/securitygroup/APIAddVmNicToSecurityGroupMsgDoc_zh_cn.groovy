package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIAddVmNicToSecurityGroupEvent

doc {
	title "AddVmNicToSecurityGroup"

	category "securityGroup"

	desc """用户可以使用AddVmNicToSecurityGroup来添加虚拟机网卡到安全组"""

	rest {
		request {
			url "POST /v1/security-groups/{securityGroupUuid}/vm-instances/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddVmNicToSecurityGroupMsg.class

			desc """用户可以使用AddVmNicToSecurityGroup来添加虚拟机网卡到安全组"""

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
					name "vmNicUuids"
					enclosedIn "params"
					desc "云主机网卡的uuid列表"
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIAddVmNicToSecurityGroupEvent.class
		}
	}
}