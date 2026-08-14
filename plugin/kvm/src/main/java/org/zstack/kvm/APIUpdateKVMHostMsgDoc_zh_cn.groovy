package org.zstack.kvm

import org.zstack.header.host.APIUpdateHostEvent

doc {
	title "UpdateKVMHost"

	category "host"

	desc """更新KVM机信息"""

	rest {
		request {
			url "PUT /v1/hosts/kvm/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateKVMHostMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn "updateKVMHost"
					desc "用户名"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "updateKVMHost"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "updateKVMHost"
					desc "ssh端口号"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "uuid"
					enclosedIn "updateKVMHost"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateKVMHost"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateKVMHost"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "managementIp"
					enclosedIn "updateKVMHost"
					desc "管理节点IP"
					location "body"
					type "String"
					optional true
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
			clz APIUpdateHostEvent.class
		}
	}
}