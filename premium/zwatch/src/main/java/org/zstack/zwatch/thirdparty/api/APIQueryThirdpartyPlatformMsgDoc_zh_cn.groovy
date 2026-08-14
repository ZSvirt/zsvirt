package org.zstack.zwatch.thirdparty.api

import org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyPlatformReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryThirdpartyPlatform"

	category "zwatch"

	desc """查询第三方报警源"""

	rest {
		request {
			url "GET /v1/zwatch/third-party/platforms"
			url "GET /v1/zwatch/third-party/platforms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryThirdpartyPlatformMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryThirdpartyPlatformReply.class
		}
	}
}