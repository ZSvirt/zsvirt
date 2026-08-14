package org.zstack.header.longjob

import org.zstack.header.longjob.APISubmitLongJobEvent

doc {
	title "SubmitLongJob"

	category "longjob"

	desc """提交长任务"""

	rest {
		request {
			url "POST /v1/longjobs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISubmitLongJobMsg.class

			desc """提交长任务"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 2.2.4"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 2.2.4"
				}
				column {
					name "jobName"
					enclosedIn "params"
					desc "任务名称"
					location "body"
					type "String"
					optional false
					since "zsv 2.2.4"
				}
				column {
					name "jobData"
					enclosedIn "params"
					desc "任务数据"
					location "body"
					type "String"
					optional false
					since "zsv 2.2.4"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
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
				column {
					name "targetResourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
			}
		}

		response {
			clz APISubmitLongJobEvent.class
		}
	}
}