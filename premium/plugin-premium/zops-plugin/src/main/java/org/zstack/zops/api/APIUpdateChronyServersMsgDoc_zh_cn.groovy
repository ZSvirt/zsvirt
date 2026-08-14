package org.zstack.zops.api

import org.zstack.zops.api.APIUpdateChronyServersEvent

doc {
	title "UpdateChronyServers"

	category "zops"

	desc """修改chrony时间源服务器"""

	rest {
		request {
			url "PUT /v1/zops/chrony/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateChronyServersMsg.class

			desc """"""

			params {

				column {
					name "internalHostnames"
					enclosedIn "updateChronyServers"
					desc "内部时间源集合"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "externalHostnames"
					enclosedIn "updateChronyServers"
					desc "外部时间源集合"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIUpdateChronyServersEvent.class
		}
	}
}