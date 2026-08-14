package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmBootOrderReply

doc {
	title "获得云主机启动设备列表(GetVmBootOrder)"

	category "vmInstance"

	desc """获得一个云主机的启动设备列表"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/boot-orders"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmBootOrderMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetVmBootOrderReply.class
		}
	}
}