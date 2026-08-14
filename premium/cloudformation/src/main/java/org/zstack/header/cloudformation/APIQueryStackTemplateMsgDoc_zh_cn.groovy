package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIQueryStackTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryStackTemplate"

	category "cloudformation"

	desc """查询资源编排模板列表"""

	rest {
		request {
			url "GET /v1/cloudformation/template"
			url "GET /v1/cloudformation/template/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryStackTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryStackTemplateReply.class
		}
	}
}