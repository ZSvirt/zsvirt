package org.zstack.header.host

import org.zstack.header.host.APIGetClusterHostNetworkFactsReply

doc {
	title "GetClusterHostNetworkFacts"

	category "cluster"

	desc """获取集群物理机物理网络信息"""

	rest {
		request {
			url "GET /v1/cluster/hosts-network-facts/{clusterUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetClusterHostNetworkFactsMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "3.13.6"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.13.6"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.13.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.13.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.13.6"
				}
			}
		}

		response {
			clz APIGetClusterHostNetworkFactsReply.class
		}
	}
}