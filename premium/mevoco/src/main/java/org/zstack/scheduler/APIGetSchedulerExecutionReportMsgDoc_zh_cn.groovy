package org.zstack.scheduler

import org.zstack.scheduler.APIGetSchedulerExecutionReportReply

doc {
	title "GetSchedulerExecutionReport"

	category "scheduler"

	desc """获取定时任务执行报告"""

	rest {
		request {
			url "GET /v1/scheduler/report"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetSchedulerExecutionReportMsg.class

			desc """"""

			params {

				column {
					name "startTime"
					enclosedIn ""
					desc "报告开始时间"
					location "query"
					type "long"
					optional false
					since "3.9.0"
				}
				column {
					name "intervalTimeUnit"
					enclosedIn ""
					desc "间隔时间单位"
					location "query"
					type "String"
					optional false
					since "3.9.0"
					values ("Hour","Day","Month")
				}
				column {
					name "range"
					enclosedIn ""
					desc "报告时间范围"
					location "query"
					type "int"
					optional false
					since "3.9.0"
				}
				column {
					name "schedulerJobTypes"
					enclosedIn ""
					desc "定时任务类型"
					location "query"
					type "List"
					optional false
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIGetSchedulerExecutionReportReply.class
		}
	}
}