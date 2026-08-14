package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIPutMetricDataEvent

doc {
	title "PutMetricData"

	category "zwatch"

	desc """添加自定义监控数据"""

	rest {
		request {
			url "POST /v1/zwatch/metrics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPutMetricDataMsg.class

			desc """"""

			params {

				column {
					name "namespace"
					enclosedIn "params"
					desc "自定义名字空间名称"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "data"
					enclosedIn "params"
					desc "数据"
					location "body"
					type "List"
					optional false
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIPutMetricDataEvent.class
		}
	}
}