package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetPrometheusMetricLabelValueReply

doc {
	title "GetPrometheusMetricLabelValue"

	category "zwatch"

	desc """获取指定标签的监控项数据"""

	rest {
		request {
			url "GET /v1/zwatch/metrics/prometheus/label-values"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPrometheusMetricLabelValueMsg.class

			desc """"""

			params {

				column {
					name "namespace"
					enclosedIn ""
					desc "名字空间名称"
					location "query"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "metricName"
					enclosedIn ""
					desc "监控指标名称"
					location "query"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "startTime"
					enclosedIn ""
					desc "开始时间"
					location "query"
					type "Long"
					optional true
					since "4.10.20"
				}
				column {
					name "endTime"
					enclosedIn ""
					desc "结束时间"
					location "query"
					type "Long"
					optional true
					since "4.10.20"
				}
				column {
					name "labelNames"
					enclosedIn ""
					desc "要获取值的标签名列表"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "filterLabels"
					enclosedIn ""
					desc "标签过滤器列表，例如可以指定标签HostUuid=e47f7145f4cd4fca8e2856038ecdf3e1来选择特定物理机的，labelNames中指定标签的值"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIGetPrometheusMetricLabelValueReply.class
		}
	}
}