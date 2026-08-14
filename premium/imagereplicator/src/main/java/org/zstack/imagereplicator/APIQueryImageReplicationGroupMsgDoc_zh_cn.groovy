package org.zstack.imagereplicator

import org.zstack.imagereplicator.APIQueryImageReplicationGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryImageReplicationGroup"

	category "imagereplicator"

	desc """查询镜像复制组"""

	rest {
		request {
			url "GET /v1/image-replication-groups"
			url "GET /v1/image-replication-groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryImageReplicationGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryImageReplicationGroupReply.class
		}
	}
}