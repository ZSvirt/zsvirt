package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmUptimeReply

doc {
	title "GetVmUptime"

	category "vmInstance"

	desc """获取虚拟机开机时间"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/uptime"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmUptimeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.18.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
			}
		}

		response {
			clz APIGetVmUptimeReply.class
		}
	}
}