package org.zstack.core.errorcode

import org.zstack.core.errorcode.APIGetElaborationCategoriesReply

doc {
	title "GetElaborationCategories"

	category "errorcode"

	desc """查看错误码目录列表"""

	rest {
		request {
			url "GET /v1/errorcode/elaborations/categories"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetElaborationCategoriesMsg.class

			desc """查看错误码目录列表"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.3.0"
				}
			}
		}

		response {
			clz APIGetElaborationCategoriesReply.class
		}
	}
}