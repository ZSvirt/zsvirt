package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetAllMetricMetadataReply

doc {
	title "GetAllMetricMetadata"

	category "zwatch"

	desc """获取所有监控项元数据"""

	rest {
		request {
			url "GET /v1/zwatch/metrics/meta-data"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAllMetricMetadataMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn ""
					desc "监控指标名称"
					location "query"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "namespace"
					enclosedIn ""
					desc "监控指标命名空间"
					location "query"
					type "String"
					optional true
					since "3.9.0"
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
			}
		}

		response {
			clz APIGetAllMetricMetadataReply.class
		}
	}
}