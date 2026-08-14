package org.zstack.header.simulator

import org.zstack.header.host.APIAddHostEvent

doc {
	title "AddSimulatorHost"

	category "host"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/hosts/simulators"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSimulatorHostMsg.class

			desc """"""

			params {

				column {
					name "memoryCapacity"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional false
					since "0.6"
				}
				column {
					name "cpuCapacity"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional false
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
					desc ""
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