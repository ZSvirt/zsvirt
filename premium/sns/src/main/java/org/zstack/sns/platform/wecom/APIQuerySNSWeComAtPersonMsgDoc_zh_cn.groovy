package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.APIQuerySNSWeComAtPersonReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSWeComAtPerson"

	category "sns"

	desc """查询企业微信@用户"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/we-com/at-persons"
			url "GET /v1/sns/application-endpoints/we-com/at-persons/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSWeComAtPersonMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSWeComAtPersonReply.class
		}
	}
}