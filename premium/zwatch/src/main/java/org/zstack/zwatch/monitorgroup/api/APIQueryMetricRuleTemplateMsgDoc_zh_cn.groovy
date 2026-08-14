package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryMetricRuleTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMetricRuleTemplate"

	category "zwatch"

	desc """查询资源报警模板"""

	rest {
		request {
			url "GET /v1/zwatch/monitortemplates/metricrules"
			url "GET /v1/zwatch/monitortemplates/metricrules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMetricRuleTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMetricRuleTemplateReply.class
		}
	}
}