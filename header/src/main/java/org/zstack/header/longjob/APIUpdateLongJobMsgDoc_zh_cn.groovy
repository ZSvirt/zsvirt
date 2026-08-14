package org.zstack.header.longjob

import org.zstack.header.longjob.APIUpdateLongJobEvent

doc {
	title "UpdateLongJob"

	category "longjob"

	desc """更新长任务"""

	rest {
		request {
			url "PUT /v1/longjobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateLongJobMsg.class

			desc """更新长任务"""

			params {

				column {
					name "uuid"
					enclosedIn "updateLongJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.9"
				}
				column {
					name "name"
					enclosedIn "updateLongJob"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 3.9"
				}
				column {
					name "description"
					enclosedIn "updateLongJob"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.9"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.9"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.9"
				}
			}
		}

		response {
			clz APIUpdateLongJobEvent.class
		}
	}
}