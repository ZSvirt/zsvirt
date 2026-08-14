package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIGetFactoryModeStateReply

doc {
	title "GetFactoryModeState"

	category "mevoco"

	desc """获取管理节点工厂状态"""

	rest {
		request {
			url "GET /v1/management-nodes/factory-mode-state"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetFactoryModeStateMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.8"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.8"
				}
			}
		}

		response {
			clz APIGetFactoryModeStateReply.class
		}
	}
}