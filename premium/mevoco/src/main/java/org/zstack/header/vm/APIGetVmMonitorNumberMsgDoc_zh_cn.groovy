package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmMonitorNumberReply

doc {
	title "GetVmMonitorNumber"

	category "mevoco"

	desc """获取vm支持的屏幕个数"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/monitorNumber"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmMonitorNumberMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVmMonitorNumberReply.class
		}
	}
}