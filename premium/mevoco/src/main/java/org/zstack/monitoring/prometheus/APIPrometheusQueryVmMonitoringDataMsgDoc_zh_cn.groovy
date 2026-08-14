package org.zstack.monitoring.prometheus

import org.zstack.monitoring.prometheus.APIPrometheusQueryVmMonitoringDataReply

doc {
	title "PrometheusQueryVmMonitoringData"

	category "prometheus"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/prometheus/vm-instances"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPrometheusQueryVmMonitoringDataMsg.class

			desc """"""

			params {

				column {
					name "vmUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "instant"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "0.6"
				}
				column {
					name "startTime"
					enclosedIn ""
					desc ""
					location "query"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "endTime"
					enclosedIn ""
					desc ""
					location "query"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "step"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "expression"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "relativeTime"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
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
			clz APIPrometheusQueryVmMonitoringDataReply.class
		}
	}
}