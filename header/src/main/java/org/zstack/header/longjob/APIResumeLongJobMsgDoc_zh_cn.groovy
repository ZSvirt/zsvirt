package org.zstack.header.longjob

import org.zstack.header.longjob.APIResumeLongJobEvent

doc {
	title "ResumeLongJob"

	category "longjob"

	desc """恢复运行长任务"""

	rest {
		request {
			url "PUT /v1/longjobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIResumeLongJobMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "resumeLongJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.9.0"
				}
			}
		}

		response {
			clz APIResumeLongJobEvent.class
		}
	}
}