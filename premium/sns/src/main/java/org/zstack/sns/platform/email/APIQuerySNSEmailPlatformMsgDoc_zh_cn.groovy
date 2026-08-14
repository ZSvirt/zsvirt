package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APIQuerySNSEmailPlatformReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSEmailPlatform"

	category "sns"

	desc """查询SNS邮件平台"""

	rest {
		request {
			url "GET /v1/sns/application-platforms/email"
			url "GET /v1/sns/application-platforms/email/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSEmailPlatformMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSEmailPlatformReply.class
		}
	}
}