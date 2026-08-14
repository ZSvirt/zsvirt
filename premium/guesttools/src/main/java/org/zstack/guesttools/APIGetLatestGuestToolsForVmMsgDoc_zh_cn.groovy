package org.zstack.guesttools

import org.zstack.guesttools.APIGetLatestGuestToolsForVmReply

doc {
	title "GetLatestGuestToolsForVm"

	category "guest.tools"

	desc """获取云主机可用的最新增强工具"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/latest-guest-tools"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetLatestGuestToolsForVmMsg.class

			desc """获取云主机可用的最新增强工具"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIGetLatestGuestToolsForVmReply.class
		}
	}
}