package org.zstack.header.cluster

import org.zstack.header.cluster.APIChangeClusterStateEvent

doc {
	title "ChangeClusterState"

	category "cluster"

	desc """管理员可以使用ChangeClusterState来改变一个集群的可用状态"""

	rest {
		request {
			url "PUT /v1/clusters/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeClusterStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeClusterState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeClusterState"
					desc "可用状态触发事件"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					values ("enable","disable")
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
			clz APIChangeClusterStateEvent.class
		}
	}
}