package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIQueryExternalServiceConfigurationReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询外部服务配置"

	category "externalService"

	desc """查询外部服务配置"""

	rest {
		request {
			url "GET /v1/external/service/configurations"
			url "GET /v1/external/service/configurations/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryExternalServiceConfigurationMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryExternalServiceConfigurationReply.class
		}
	}
}