package org.zstack.drs.api

import org.zstack.drs.api.APICreateClusterDRSEvent

doc {
	title "CreateClusterDRS"

	category "drs"

	desc """创建集群DRS"""

	rest {
		request {
			url "POST /v1/clusters/{clusterUuid}/drs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateClusterDRSMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "automationLevel"
					enclosedIn "params"
					desc "调度是手动触发还是自动触发"
					location "body"
					type "String"
					optional false
					since "4.0.0"
					values ("Automatic","Manual")
				}
				column {
					name "thresholds"
					enclosedIn "params"
					desc "阈值判断项目"
					location "body"
					type "List"
					optional false
					since "4.0.0"
				}
				column {
					name "thresholdDuration"
					enclosedIn "params"
					desc "阈值判断时长。系统将查询从现在往前 thresholdDuration 的时段的负载数据进行统计，算出的结果和阈值比较来判断集群是否需要调度"
					location "body"
					type "Integer"
					optional false
					since "4.0.0"
				}
				column {
					name "defaultEnable"
					enclosedIn "params"
					desc "集群DRS的启用状态，刚创建完是启用还是禁用"
					location "body"
					type "boolean"
					optional true
					since "4.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
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
			clz APICreateClusterDRSEvent.class
		}
	}
}