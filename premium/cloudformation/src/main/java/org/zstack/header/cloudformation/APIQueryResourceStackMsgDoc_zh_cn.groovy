package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIQueryResourceStackReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryResourceStack"

	category "cloudformation"

	desc """查询资源编排堆栈列表"""

	rest {
		request {
			url "GET /v1/cloudformation/stack"
			url "GET /v1/cloudformation/stack/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryResourceStackMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryResourceStackReply.class
		}
	}
}