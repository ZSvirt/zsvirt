package org.zstack.header.cluster

import org.zstack.header.cluster.APIUpdateClusterEvent

doc {
	title "UpdateCluster"

	category "cluster"

	desc """更新集群"""

	rest {
		request {
			url "PUT /v1/clusters/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateClusterMsg.class

			desc """更新集群"""

			params {

				column {
					name "uuid"
					enclosedIn "updateCluster"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateCluster"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateCluster"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIUpdateClusterEvent.class
		}
	}
}