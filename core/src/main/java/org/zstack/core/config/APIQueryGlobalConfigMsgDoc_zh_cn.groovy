package org.zstack.core.config

import org.zstack.core.config.APIQueryGlobalConfigReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryGlobalConfig"

	category "globalConfig"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/global-configurations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryGlobalConfigMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryGlobalConfigReply.class
		}
	}
}