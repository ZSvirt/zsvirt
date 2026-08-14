package org.zstack.scheduler

import org.zstack.scheduler.APICreateSchedulerTriggerEvent

doc {
	title "CreateSchedulerTrigger"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/scheduler/triggers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSchedulerTriggerMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "schedulerInterval"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "repeatCount"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "startTime"
					enclosedIn "params"
					desc "开始时间(Unix 时间戳，单位为秒)"
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "stopTime"
					enclosedIn "params"
					desc "结束时间(Unix 时间戳，单位为秒)"
					location "body"
					type "Long"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "schedulerType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("simple","cron")
				}
				column {
					name "cron"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateSchedulerTriggerEvent.class
		}
	}
}