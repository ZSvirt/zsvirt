package org.zstack.scheduler

import org.zstack.scheduler.APICreateSchedulerJobGroupEvent

doc {
	title "CreateSchedulerJobGroup"

	category "scheduler"

	desc """创建定时任务组"""

	rest {
		request {
			url "POST /v1/scheduler/jobgroups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSchedulerJobGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "定时任务的类型"
					location "body"
					type "String"
					optional false
					since "3.4.0"
					values ("startVm","stopVm","rebootVm","volumeSnapshot","volumeBackup","vmBackup","databaseBackup")
				}
				column {
					name "parameters"
					enclosedIn "params"
					desc "定时任务参数"
					location "body"
					type "Map"
					optional true
					since "3.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.4.0"
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
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateSchedulerJobGroupEvent.class
		}
	}
}