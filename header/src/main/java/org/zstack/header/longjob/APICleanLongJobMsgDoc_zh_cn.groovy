package org.zstack.header.longjob

import org.zstack.header.longjob.APICleanLongJobEvent

doc {
	title "CleanLongJob"

	category "longjob"

	desc """清理Long Job"""

	rest {
		request {
			url "PUT /v1/longjobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanLongJobMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "cleanLongJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.13.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.13.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.13.0"
				}
			}
		}

		response {
			clz APICleanLongJobEvent.class
		}
	}
}