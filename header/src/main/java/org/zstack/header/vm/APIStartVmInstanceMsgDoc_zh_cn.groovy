package org.zstack.header.vm

import org.zstack.header.vm.APIStartVmInstanceEvent

doc {
	title "启动云主机(StartVmInstance)"

	category "vmInstance"

	desc """启动一个云主机"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIStartVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "startVmInstance"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "clusterUuid"
					enclosedIn "startVmInstance"
					desc "集群UUID。若指定，云主机将在该集群启动"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "hostUuid"
					enclosedIn "startVmInstance"
					desc "物理机UUID。若指定，云主机将在该物理机启动。该字段覆盖`clusterUuid`字段"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIStartVmInstanceEvent.class
		}
	}
}