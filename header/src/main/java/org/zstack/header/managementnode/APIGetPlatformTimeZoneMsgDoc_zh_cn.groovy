package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIGetPlatformTimeZoneReply

doc {
	title "GetPlatformTimeZone"

	category "mevoco"

	desc """获取管理节点当前的时区信息"""

	rest {
		request {
			url "GET /v1/management-nodes/platform-timezone"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPlatformTimeZoneMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
			}
		}

		response {
			clz APIGetPlatformTimeZoneReply.class
		}
	}
}