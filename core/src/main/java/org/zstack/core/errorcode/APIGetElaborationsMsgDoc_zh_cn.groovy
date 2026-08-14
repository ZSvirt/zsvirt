package org.zstack.core.errorcode

import org.zstack.core.errorcode.APIGetElaborationsReply

doc {
	title "GetElaborations"

	category "errorcode"

	desc """查看系统错误码"""

	rest {
		request {
			url "GET /v1/errorcode/elaborations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetElaborationsMsg.class

			desc """查看系统错误码"""

			params {

				column {
					name "category"
					enclosedIn ""
					desc "错误码目录"
					location "query"
					type "String"
					optional true
					since "3.3.0"
				}
				column {
					name "regex"
					enclosedIn ""
					desc "错误码关键字"
					location "query"
					type "String"
					optional true
					since "3.3.0"
				}
				column {
					name "code"
					enclosedIn ""
					desc "错误代码，与category一起使用"
					location "query"
					type "String"
					optional true
					since "3.6.0"
				}
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
			clz APIGetElaborationsReply.class
		}
	}
}