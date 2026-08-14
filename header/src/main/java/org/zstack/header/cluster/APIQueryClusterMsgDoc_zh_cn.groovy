package org.zstack.header.cluster

import org.zstack.header.cluster.APIQueryClusterReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryCluster"

	category "cluster"

	desc """管理员可以使用QueryCluster命令来查询集群"""

	rest {
		request {
			url "GET /v1/clusters"
			url "GET /v1/clusters/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryClusterMsg.class

			desc """管理员可以使用QueryCluster命令来查询集群"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryClusterReply.class
		}
	}
}