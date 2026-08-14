package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIDeleteResourceStackEvent

doc {
	title "DeleteResourceStack"

	category "cloudformation"

	desc """删除资源编排堆栈"""

	rest {
		request {
			url "DELETE /v1/cloudformation/stack/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteResourceStackMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.5.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.5.0"
				}
			}
		}

		response {
			clz APIDeleteResourceStackEvent.class
		}
	}
}