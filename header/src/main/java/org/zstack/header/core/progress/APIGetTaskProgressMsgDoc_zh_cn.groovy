package org.zstack.header.core.progress

import org.zstack.header.core.progress.APIGetTaskProgressReply

doc {
	title "GetTaskProgress"

	category "core.progress"

	desc """获取任务进度"""

	rest {
		request {
			url "GET /v1/task-progresses/{apiId}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetTaskProgressMsg.class

			desc """"""

			params {

				column {
					name "apiId"
					enclosedIn ""
					desc "任务对应的API ID"
					location "url"
					type "String"
					optional true
					since "zsv 1.11"
				}
				column {
					name "all"
					enclosedIn ""
					desc "指定获取所有进度信息"
					location "query"
					type "boolean"
					optional true
					since "zsv 1.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 1.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 1.11"
				}
			}
		}

		response {
			clz APIGetTaskProgressReply.class
		}
	}
}