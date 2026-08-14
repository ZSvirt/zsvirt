package org.zstack.scheduler

import org.zstack.scheduler.APIUpdateSchedulerTriggerEvent

doc {
	title "UpdateSchedulerTrigger"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/scheduler/triggers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSchedulerTriggerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSchedulerTrigger"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateSchedulerTrigger"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateSchedulerTrigger"
					desc "资源的详细描述"
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
					name "schedulerInterval"
					enclosedIn "updateSchedulerTrigger"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "repeatCount"
					enclosedIn "updateSchedulerTrigger"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "startTime"
					enclosedIn "updateSchedulerTrigger"
					desc "开始时间(Unix 时间戳，单位为秒)"
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "stopTime"
					enclosedIn "updateSchedulerTrigger"
					desc "结束时间(Unix 时间戳，单位为秒)"
					location "body"
					type "Long"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "cron"
					enclosedIn "updateSchedulerTrigger"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "schedulerType"
					enclosedIn "updateSchedulerTrigger"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("cron","simple")
				}
			}
		}

		response {
			clz APIUpdateSchedulerTriggerEvent.class
		}
	}
}