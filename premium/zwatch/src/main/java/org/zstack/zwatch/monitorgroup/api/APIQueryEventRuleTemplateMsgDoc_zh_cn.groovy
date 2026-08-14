package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryEventRuleTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryEventRuleTemplate"

	category "zwatch"

	desc """查询事件报警模板列表"""

	rest {
		request {
			url "GET /v1/zwatch/monitortemplates/evenrules"
			url "GET /v1/zwatch/monitortemplates/evenrules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEventRuleTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryEventRuleTemplateReply.class
		}
	}
}