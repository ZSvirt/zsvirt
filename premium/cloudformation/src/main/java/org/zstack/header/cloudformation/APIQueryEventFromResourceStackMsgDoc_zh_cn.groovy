package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIQueryEventFromResourceStackReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryEventFromResourceStack"

	category "cloudformation"

	desc """查询资源编排堆栈中的事件列表"""

	rest {
		request {
			url "GET /v1/cloudformation/event"
			url "GET /v1/cloudformation/event/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEventFromResourceStackMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryEventFromResourceStackReply.class
		}
	}
}