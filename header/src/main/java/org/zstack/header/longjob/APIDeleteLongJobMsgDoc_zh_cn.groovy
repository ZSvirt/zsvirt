package org.zstack.header.longjob

import org.zstack.header.longjob.APIDeleteLongJobEvent

doc {
	title "DeleteLongJob"

	category "longjob"

	desc """删除长任务"""

	rest {
		request {
			url "DELETE /v1/longjobs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteLongJobMsg.class

			desc """删除长任务"""

			params {

				column {
					name "uuid"
					enclosedIn ""
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
					location "query"
					type "List"
					optional true
					since "zsv 2.2.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 2.2.4"
				}
			}
		}

		response {
			clz APIDeleteLongJobEvent.class
		}
	}
}