package org.zstack.header.managementnode

import org.zstack.header.managementnode.APIQueryManagementNodeReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryManagementNode"

	category "managementNode"

	desc """查询管理节点"""

	rest {
		request {
			url "GET /v1/management-nodes"
			url "GET /v1/management-nodes/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryManagementNodeMsg.class

			desc """查询管理节点"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryManagementNodeReply.class
		}
	}
}