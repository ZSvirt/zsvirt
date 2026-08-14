package org.zstack.monitoring.prometheus

import org.zstack.monitoring.prometheus.APIPrometheusQueryMetadataReply

doc {
	title "PrometheusQueryMetadata"

	category "prometheus"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/prometheus/meta-data"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPrometheusQueryMetadataMsg.class

			desc """"""

			params {

				column {
					name "matches"
					enclosedIn ""
					desc "匹配条件"
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
			clz APIPrometheusQueryMetadataReply.class
		}
	}
}