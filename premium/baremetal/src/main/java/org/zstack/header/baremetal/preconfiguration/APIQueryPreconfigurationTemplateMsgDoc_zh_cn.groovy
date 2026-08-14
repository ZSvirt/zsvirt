package org.zstack.header.baremetal.preconfiguration

import org.zstack.header.baremetal.preconfiguration.APIQueryPreconfigurationTemplatesReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryPreconfigurationTemplate"

	category "baremetal.preconfiguration"

	desc """查询预配置模板"""

	rest {
		request {
			url "GET /v1/baremetal/preconfigurations"
			url "GET /v1/baremetal/preconfigurations/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPreconfigurationTemplateMsg.class

			desc """查询预配置模板"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPreconfigurationTemplatesReply.class
		}
	}
}