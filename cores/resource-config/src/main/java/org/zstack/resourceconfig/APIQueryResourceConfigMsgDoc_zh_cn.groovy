package org.zstack.resourceconfig

import org.zstack.resourceconfig.APIQueryResourceConfigReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryResourceConfig"

	category "resourceConfig"

	desc """查询资源高级设置"""

	rest {
		request {
			url "GET /v1/resource-configurations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryResourceConfigMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryResourceConfigReply.class
		}
	}
}