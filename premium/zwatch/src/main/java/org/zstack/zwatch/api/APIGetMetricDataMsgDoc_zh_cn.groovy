package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetMetricDataReply

doc {
	title "GetMetricData"

	category "zwatch"

	desc """获取监控数据"""

	rest {
		request {
			url "GET /v1/zwatch/metrics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetMetricDataMsg.class

			desc """"""

			params {

				column {
					name "namespace"
					enclosedIn ""
					desc "名字空间"
					location "query"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "metricName"
					enclosedIn ""
					desc "监控项"
					location "query"
					type "String"
					optional false
					since "2.3"
				}
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
					name "period"
					enclosedIn ""
					desc "数据精度"
					location "query"
					type "Integer"
					optional true
					since "2.3"
				}
				column {
					name "labels"
					enclosedIn ""
					desc "过滤标签"
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
					name "functions"
					enclosedIn ""
					desc "函数列表"
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
					name "valueConditions"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetMetricDataReply.class
		}
	}
}