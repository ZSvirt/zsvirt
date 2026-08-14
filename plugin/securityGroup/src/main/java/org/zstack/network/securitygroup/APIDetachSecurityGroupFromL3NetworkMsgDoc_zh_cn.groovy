package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIDetachSecurityGroupFromL3NetworkEvent

doc {
	title "DetachSecurityGroupFromL3Network"

	category "securityGroup"

	desc """用户可以使用DetachSecurityGroupFromL3Network来从一个L3网络卸载一个安全组,卸载后, 所有的规则都会从这个L3网络上的并且在这个安全组中的虚拟机网卡上删除. 这个命令是异步执行的, 在它返回后可能规则仍然没有对所有虚拟机网卡生效"""

	rest {
		request {
			url "DELETE /v1/security-groups/{securityGroupUuid}/l3-networks/{l3NetworkUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachSecurityGroupFromL3NetworkMsg.class

			desc """用户可以使用DetachSecurityGroupFromL3Network来从一个L3网络卸载一个安全组,卸载后, 所有的规则都会从这个L3网络上的并且在这个安全组中的虚拟机网卡上删除. 这个命令是异步执行的, 在它返回后可能规则仍然没有对所有虚拟机网卡生效"""

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
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
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
			clz APIDetachSecurityGroupFromL3NetworkEvent.class
		}
	}
}