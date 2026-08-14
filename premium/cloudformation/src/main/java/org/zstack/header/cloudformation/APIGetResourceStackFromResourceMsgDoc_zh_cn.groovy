package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIGetResourceStackFromResourceReply

doc {
	title "GetResourceStackFromResource"

	category "cloudformation"

	desc """获取资源对应的资源栈"""

	rest {
		request {
			url "GET /v1/cloudformation/resources/stack"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceStackFromResourceMsg.class

			desc """"""

			params {

				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
					location "query"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIGetResourceStackFromResourceReply.class
		}
	}
}