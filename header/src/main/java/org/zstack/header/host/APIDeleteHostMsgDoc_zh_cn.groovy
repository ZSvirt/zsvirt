package org.zstack.header.host

import org.zstack.header.host.APIDeleteHostEvent

doc {
	title "DeleteHost"

	category "host"

	desc """删除物理机"""

	rest {
		request {
			url "DELETE /v1/hosts/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteHostMsg.class

			desc """删除一个物理机"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteHostEvent.class
		}
	}
}