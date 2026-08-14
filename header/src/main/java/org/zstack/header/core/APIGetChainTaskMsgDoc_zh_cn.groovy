package org.zstack.header.core

import org.zstack.header.core.APIGetChainTaskReply

doc {
	title "GetChainTask"

	category "core"

	desc """获取任务队列详情"""

	rest {
		request {
			url "GET /v1/core/task-details"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetChainTaskMsg.class

			desc """"""

			params {

				column {
					name "syncSignatures"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
			}
		}

		response {
			clz APIGetChainTaskReply.class
		}
	}
}