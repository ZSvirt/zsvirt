package org.zstack.core.errorcode

import org.zstack.core.errorcode.APIReloadElaborationEvent

doc {
	title "ReloadElaboration"

	category "errorcode"

	desc """重新加载系统错误码文件"""

	rest {
		request {
			url "PUT /v1/errorcode/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReloadElaborationMsg.class

			desc """重新加载系统错误码文件"""

			params {

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
			clz APIReloadElaborationEvent.class
		}
	}
}