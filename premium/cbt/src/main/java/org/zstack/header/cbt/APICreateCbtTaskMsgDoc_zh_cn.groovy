package org.zstack.header.cbt

import org.zstack.header.cbt.APICreateCbtTaskEvent

doc {
	title "CreateCbtTask"

	category "cbt"

	desc """创建CBT任务"""

	rest {
		request {
			url "POST /v1/cbt-task/create"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateCbtTaskMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.10.10"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.10.10"
				}
				column {
					name "taskType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "4.10.10"
				}
				column {
					name "resourceUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "4.10.10"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.10.10"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
			}
		}

		response {
			clz APICreateCbtTaskEvent.class
		}
	}
}