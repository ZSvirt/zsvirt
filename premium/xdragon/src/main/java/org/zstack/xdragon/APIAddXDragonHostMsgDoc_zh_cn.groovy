package org.zstack.xdragon

import org.zstack.header.host.APIAddHostEvent

doc {
	title "AddXDragonHost"

	category "host"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/hosts/xdragon"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddXDragonHostMsg.class

			desc """"""

			params {

				column {
					name "username"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "cpuSockets"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "totalPhysicalMemory"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "params"
					desc ""
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
					desc ""
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
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
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
			clz APIAddHostEvent.class
		}
	}
}