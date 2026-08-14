package org.zstack.managements.api.ha2

import org.zstack.managements.api.ha2.APIZSha2DemoteEvent

doc {
	title "ZSha2Demote"

	category "managementsHa2"

	desc """降级双管中当前管理节点"""

	rest {
		request {
			url "PUT /v1/management-nodes/zsha2/demote"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIZSha2DemoteMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIZSha2DemoteEvent.class
		}
	}
}