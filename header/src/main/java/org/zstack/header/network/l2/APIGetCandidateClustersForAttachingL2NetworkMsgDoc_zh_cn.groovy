package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIGetCandidateClustersForAttachingL2NetworkReply

doc {
	title "获取二层网络允许加载的集群"

	category "二层网络"

	desc """获取二层网络允许加载的集群"""

	rest {
		request {
			url "GET /v1/l2-networks/{l2NetworkUuid}/cluster-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateClustersForAttachingL2NetworkMsg.class

			desc """"""

			params {

				column {
					name "l2NetworkUuid"
					enclosedIn ""
					desc "二层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.10.0"
				}
				column {
					name "clusterTypes"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
			}
		}

		response {
			clz APIGetCandidateClustersForAttachingL2NetworkReply.class
		}
	}
}