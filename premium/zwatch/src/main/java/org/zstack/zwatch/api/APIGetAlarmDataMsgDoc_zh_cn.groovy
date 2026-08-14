package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetAlarmDataReply

doc {
	title "GetAlarmData"

	category "zwatch"

	desc """获取报警器历史记录"""

	rest {
		request {
			url "GET /v1/zwatch/alarm-histories"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAlarmDataMsg.class

			desc """"""

			params {

				column {
					name "startTime"
					enclosedIn ""
					desc "起始时间"
					location "query"
					type "Long"
					optional true
					since "2.3"
				}
				column {
					name "endTime"
					enclosedIn ""
					desc "结束时间"
					location "query"
					type "Long"
					optional true
					since "2.3"
				}
				column {
					name "limit"
					enclosedIn ""
					desc "返回记录条数数量"
					location "query"
					type "Integer"
					optional true
					since "2.3"
				}
				column {
					name "conditions"
					enclosedIn ""
					desc "条件列表，用于过滤结果"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "count"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "0.6"
				}
				column {
					name "excludeOtherAccount"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "0.6"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "endpointUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetAlarmDataReply.class
		}
	}
}