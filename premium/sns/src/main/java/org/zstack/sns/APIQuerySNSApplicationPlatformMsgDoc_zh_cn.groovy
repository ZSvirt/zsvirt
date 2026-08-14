package org.zstack.sns

import org.zstack.sns.APIQuerySNSApplicationPlatformReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSApplicationPlatform"

	category "sns"

	desc """查询SNS应用平台"""

	rest {
		request {
			url "GET /v1/sns/application-platforms"
			url "GET /v1/sns/application-platforms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSApplicationPlatformMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSApplicationPlatformReply.class
		}
	}
}