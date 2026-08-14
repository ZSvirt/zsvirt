package org.zstack.storage.device.multipath

import org.zstack.storage.device.multipath.APIGetHostMultipathTopologyReply

doc {
	title "GetHostMultipathTopology"

	category "storage.device"

	desc """获取物理机多路径拓扑"""

	rest {
		request {
			url "GET /v1/storage-devices/multipath/topology"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostMultipathTopologyMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional false
					since "4.1.0"
				}
				column {
					name "lunUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional false
					since "4.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.1.0"
				}
			}
		}

		response {
			clz APIGetHostMultipathTopologyReply.class
		}
	}
}