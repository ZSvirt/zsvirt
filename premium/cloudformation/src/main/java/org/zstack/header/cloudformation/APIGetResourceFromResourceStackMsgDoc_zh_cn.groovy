package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIGetResourceFromResourceStackReply

doc {
	title "GetResourceFromResourceStack"

	category "cloudformation"

	desc """查看资源编排堆栈中的资源列表"""

	rest {
		request {
			url "GET /v1/cloudformation/stack/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceFromResourceStackMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "堆栈的UUID"
					location "query"
					type "String"
					optional false
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
			clz APIGetResourceFromResourceStackReply.class
		}
	}
}