package org.zstack.monitoring.prometheus

import org.zstack.monitoring.prometheus.APIPrometheusQueryLabelValuesReply

doc {
	title "PrometheusQueryLabelValues"

	category "prometheus"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/prometheus/labels"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPrometheusQueryLabelValuesMsg.class

			desc """"""

			params {

				column {
					name "labels"
					enclosedIn ""
					desc "标签值列表"
					location "query"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIPrometheusQueryLabelValuesReply.class
		}
	}
}