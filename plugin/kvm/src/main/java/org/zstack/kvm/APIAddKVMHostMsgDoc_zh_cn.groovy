package org.zstack.kvm

import org.zstack.header.host.APIAddHostEvent

doc {
	title "AddKVMHost"

	category "host"

	desc """添加KVM主机"""

	rest {
		request {
			url "POST /v1/hosts/kvm"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddKVMHostMsg.class

			desc """添加KVM主机"""

			params {

				column {
					name "username"
					enclosedIn "params"
					desc "ssh用户名"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "ssh密码"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "params"
					desc "ssh端口号"
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "managementIp"
					enclosedIn "params"
					desc "物理机管理节点IP"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddHostEvent.class
		}
	}
}