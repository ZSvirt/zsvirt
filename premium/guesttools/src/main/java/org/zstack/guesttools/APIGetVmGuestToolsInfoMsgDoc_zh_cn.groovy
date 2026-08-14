package org.zstack.guesttools

import org.zstack.guesttools.APIGetVmGuestToolsInfoReply

doc {
	title "GetVmGuestToolsInfo"

	category "guest.tools"

	desc """获取云主机内部增强工具的信息"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/guest-tools-infos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmGuestToolsInfoMsg.class

			desc """获取云主机内部增强工具的信息"""

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
				column {
					name "debug"
					enclosedIn ""
					desc ""
					location "query"
					type "Set"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVmGuestToolsInfoReply.class
		}
	}
}