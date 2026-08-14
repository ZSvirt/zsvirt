package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetMetricLabelValueReply

doc {
	title "GetMetricLabelValue"

	category "zwatch"

	desc """获取监控项标签值"""

	rest {
		request {
			url "GET /v1/zwatch/metrics/label-values"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetMetricLabelValueMsg.class

			desc """"""

			params {

				column {
					name "namespace"
					enclosedIn ""
					desc "名字空间名称"
					location "query"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "metricName"
					enclosedIn ""
					desc "监控指标名称"
					location "query"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "labelNames"
					enclosedIn ""
					desc "要获取值得标签名列表"
					location "query"
					type "List"
					optional false
					since "2.3"
				}
				column {
					name "filterLabels"
					enclosedIn ""
					desc "标签过滤器列表，例如可以指定标签HostUuid=e47f7145f4cd4fca8e2856038ecdf3e1来选择特定物理机的，labelNames中指定标签的值"
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
					name "startTime"
					enclosedIn ""
					desc "起始时间"
					location "query"
					type "Long"
					optional true
					since "3.9.0"
				}
				column {
					name "endTime"
					enclosedIn ""
					desc "结束时间"
					location "query"
					type "Long"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIGetMetricLabelValueReply.class
		}
	}
}