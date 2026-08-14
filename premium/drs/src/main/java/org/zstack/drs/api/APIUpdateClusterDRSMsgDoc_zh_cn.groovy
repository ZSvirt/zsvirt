package org.zstack.drs.api

import org.zstack.drs.api.APIUpdateClusterDRSEvent

doc {
	title "UpdateClusterDRS"

	category "drs"

	desc """更新集群DRS"""

	rest {
		request {
			url "PUT /v1/clusters/drs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateClusterDRSMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateClusterDRS"
					desc "集群DRS的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "name"
					enclosedIn "updateClusterDRS"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "description"
					enclosedIn "updateClusterDRS"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "automationLevel"
					enclosedIn "updateClusterDRS"
					desc "调度是手动触发还是自动触发"
					location "body"
					type "String"
					optional true
					since "4.0.0"
					values ("Automatic","Manual")
				}
				column {
					name "thresholds"
					enclosedIn "updateClusterDRS"
					desc "阈值判断项目"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "thresholdDuration"
					enclosedIn "updateClusterDRS"
					desc "阈值判断时长。系统将查询从现在往前 thresholdDuration 的时段的负载数据进行统计，算出的结果和阈值比较来判断集群是否需要调度"
					location "body"
					type "Integer"
					optional true
					since "4.0.0"
				}
				column {
					name "state"
					enclosedIn "updateClusterDRS"
					desc "集群DRS的启用状态"
					location "body"
					type "String"
					optional true
					since "4.0.0"
					values ("Enabled","Disabled")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
			}
		}

		response {
			clz APIUpdateClusterDRSEvent.class
		}
	}
}