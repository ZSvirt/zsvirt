package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmvNUMATopologyReply

doc {
	title "获取云主机的vNUMA拓扑"

	category "vm"

	desc """null"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/vnuma-topology"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmvNUMATopologyMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.13.12"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.13.12"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.13.12"
				}
			}
		}

		response {
			clz APIGetVmvNUMATopologyReply.class
		}
	}
}