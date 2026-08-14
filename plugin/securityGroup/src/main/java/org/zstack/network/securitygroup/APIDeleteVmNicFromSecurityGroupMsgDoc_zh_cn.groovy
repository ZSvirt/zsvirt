package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIDeleteVmNicFromSecurityGroupEvent

doc {
	title "DeleteVmNicFromSecurityGroup"

	category "securityGroup"

	desc """用户可以使用DeleteVmNicFromSecurityGroup来从安全组删除虚拟机网卡, 这个命令是异步执行的, 在它返回后可能规则仍然没有对所有虚拟机网卡生效"""

	rest {
		request {
			url "DELETE /v1/security-groups/{securityGroupUuid}/vm-instances/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmNicFromSecurityGroupMsg.class

			desc """用户可以使用DeleteVmNicFromSecurityGroup来从安全组删除虚拟机网卡, 这个命令是异步执行的, 在它返回后可能规则仍然没有对所有虚拟机网卡生效"""

			params {

				column {
					name "securityGroupUuid"
					enclosedIn ""
					desc "安全组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "vmNicUuids"
					enclosedIn ""
					desc "网卡的uuid列表"
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
			clz APIDeleteVmNicFromSecurityGroupEvent.class
		}
	}
}