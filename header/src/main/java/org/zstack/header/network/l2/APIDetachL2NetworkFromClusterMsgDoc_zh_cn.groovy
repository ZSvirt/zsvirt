package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIDetachL2NetworkFromClusterEvent

doc {
	title "从集群上卸载二层网络(DetachL2NetworkFromCluster)"

	category "二层网络"

	desc """从集群上卸载二层网络"""

	rest {
		request {
			url "DELETE /v1/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachL2NetworkFromClusterMsg.class

			desc """"""

			params {

				column {
					name "l2NetworkUuid"
					enclosedIn ""
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "url"
					type "String"
					optional false
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
			clz APIDetachL2NetworkFromClusterEvent.class
		}
	}
}