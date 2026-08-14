package org.zstack.zwatch.alarm.sns

import org.zstack.zwatch.alarm.sns.APIQuerySNSTextTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSTextTemplate"

	category "alarm.sns"

	desc """查询报警器消息模版"""

	rest {
		request {
			url "GET /v1/zwatch/alarms/sns/text-templates"
			url "GET /v1/zwatch/alarms/sns/text-templates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSTextTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSTextTemplateReply.class
		}
	}
}