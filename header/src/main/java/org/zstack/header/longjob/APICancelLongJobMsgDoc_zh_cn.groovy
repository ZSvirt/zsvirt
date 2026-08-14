package org.zstack.header.longjob

import org.zstack.header.longjob.APICancelLongJobEvent

doc {
	title "CancelLongJob"

	category "longjob"

	desc """取消长任务"""

	rest {
		request {
			url "PUT /v1/longjobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICancelLongJobMsg.class

			desc """取消长任务"""

			params {

				column {
					name "uuid"
					enclosedIn "cancelLongJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 2.2.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 2.2.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 2.2.4"
				}
			}
		}

		response {
			clz APICancelLongJobEvent.class
		}
	}
}