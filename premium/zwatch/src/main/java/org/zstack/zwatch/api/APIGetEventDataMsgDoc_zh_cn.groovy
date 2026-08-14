package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetEventDataReply

doc {
	title "GetEventData"

	category "zwatch"

	desc """获取事件"""

	rest {
		request {
			url "GET /v1/zwatch/events"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetEventDataMsg.class

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
					desc "最大返回条数"
					location "query"
					type "Integer"
					optional true
					since "2.3"
				}
				column {
					name "labels"
					enclosedIn ""
					desc "过滤标签列表"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "latest"
					enclosedIn ""
					desc "是否只返回最近一条记录"
					location "query"
					type "boolean"
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
					name "offsetAheadOfCurrentTime"
					enclosedIn ""
					desc ""
					location "query"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "conditions"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
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
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "conditionExpression"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
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
			clz APIGetEventDataReply.class
		}
	}
}