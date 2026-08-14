package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APIQuerySNSEmailAddressReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSEmailAddress"

	category "sns"

	desc """查询接收端邮箱地址"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/emails/email-addresses"
			url "GET /v1/sns/application-endpoints/emails/email-addresses/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSEmailAddressMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSEmailAddressReply.class
		}
	}
}