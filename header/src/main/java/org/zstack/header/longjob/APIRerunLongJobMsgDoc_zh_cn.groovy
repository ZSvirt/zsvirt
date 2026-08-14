package org.zstack.header.longjob

import org.zstack.header.longjob.APIRerunLongJobEvent

doc {
	title "RerunLongJob"

	category "longjob"

	desc """重新提交长任务"""

	rest {
		request {
			url "PUT /v1/longjobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRerunLongJobMsg.class

			desc """重新提交长任务"""

			params {

				column {
					name "uuid"
					enclosedIn "rerunLongJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.0.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.0.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.0.1"
				}
			}
		}

		response {
			clz APIRerunLongJobEvent.class
		}
	}
}