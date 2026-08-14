package org.zstack.managements.api.common

import org.zstack.managements.api.common.APIGetManagementNodesStatusReply

doc {
	title "GetManagementNodesStatus"

	category "managementsHa2"

	desc """获取管理节点信息"""

	rest {
		request {
			url "GET /v1/management-nodes/status"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetManagementNodesStatusMsg.class

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
			clz APIGetManagementNodesStatusReply.class
		}
	}
}