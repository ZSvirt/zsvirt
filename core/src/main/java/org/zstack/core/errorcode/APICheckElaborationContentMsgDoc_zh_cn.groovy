package org.zstack.core.errorcode

import org.zstack.core.errorcode.APICheckElaborationContentReply

doc {
	title "CheckElaborationContent"

	category "errorcode"

	desc """检查错误码文件的格式"""

	rest {
		request {
			url "POST /v1/errorcode/elaborations/check"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckElaborationContentMsg.class

			desc """检查错误码文件的格式"""

			params {

				column {
					name "elaborateFile"
					enclosedIn "params"
					desc "要检查的文件内容，可以为一个目录"
					location "body"
					type "String"
					optional true
					since "3.3.0"
				}
				column {
					name "elaborateContent"
					enclosedIn "params"
					desc "要检查的文件内容，格式为json"
					location "body"
					type "String"
					optional true
					since "3.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.3.0"
				}
			}
		}

		response {
			clz APICheckElaborationContentReply.class
		}
	}
}