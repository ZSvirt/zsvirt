package org.zstack.managements.api.ha2

import org.zstack.managements.api.ha2.APIGetZSha2StatusReply

doc {
	title "GetZSha2Status"

	category "managementsHa2"

	desc """获取管理节点高可用信息"""

	rest {
		request {
			url "GET /v1/management-nodes/zsha2/status"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetZSha2StatusMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIGetZSha2StatusReply.class
		}
	}
}