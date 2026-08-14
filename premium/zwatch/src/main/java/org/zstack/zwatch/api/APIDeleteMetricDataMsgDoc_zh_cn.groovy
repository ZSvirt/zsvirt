package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIDeleteMetricDataEvent

doc {
	title "DeleteMetricData"

	category "zwatch"

	desc """删除监控数据"""

	rest {
		request {
			url "DELETE /v1/zwatch/metrics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteMetricDataMsg.class

			desc """"""

			params {

				column {
					name "namespace"
					enclosedIn ""
					desc "名字空间"
					location "query"
					type "String"
					optional false
					since "3.3.0"
				}
				column {
					name "metricName"
					enclosedIn ""
					desc "度量名称"
					location "query"
					type "String"
					optional false
					since "3.3.0"
				}
				column {
					name "labels"
					enclosedIn ""
					desc "过滤标签"
					location "query"
					type "List"
					optional true
					since "3.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.2.0"
				}
			}
		}

		response {
			clz APIDeleteMetricDataEvent.class
		}
	}
}