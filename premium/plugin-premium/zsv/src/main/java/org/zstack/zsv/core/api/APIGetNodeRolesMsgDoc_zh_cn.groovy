package org.zstack.zsv.core.api

import org.zstack.zsv.core.api.APIGetNodeRolesReply

doc {
	title "GetNodeRoles"

	category "ZsvStorage"

	desc """判断管理节点是否有其它的角色，比如是否为计算节点"""

	rest {
		request {
			url "GET /v1/zsv/nodes/roles"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetNodeRolesMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.7"
				}
			}
		}

		response {
			clz APIGetNodeRolesReply.class
		}
	}
}