package org.zstack.header.cluster

import org.zstack.header.cluster.APIDeleteClusterEvent

doc {
	title "DeleteCluster"

	category "cluster"

	desc """管理员可以使用DeleteCluster命令来删除一个集群"""

	rest {
		request {
			url "DELETE /v1/clusters/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteClusterMsg.class

			desc """管理员可以使用DeleteCluster命令来删除一个集群"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteClusterEvent.class
		}
	}
}