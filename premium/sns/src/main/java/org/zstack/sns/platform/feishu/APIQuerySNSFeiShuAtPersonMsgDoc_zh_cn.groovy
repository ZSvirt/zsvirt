package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.APIQuerySNSFeiShuAtPersonReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSFeiShuAtPerson"

	category "sns"

	desc """查询飞书@用户"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/feishu/at-persons"
			url "GET /v1/sns/application-endpoints/feishu/at-persons/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSFeiShuAtPersonMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSFeiShuAtPersonReply.class
		}
	}
}