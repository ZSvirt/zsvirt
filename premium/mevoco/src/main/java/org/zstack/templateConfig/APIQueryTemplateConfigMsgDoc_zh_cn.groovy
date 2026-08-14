package org.zstack.templateConfig

import org.zstack.templateConfig.APIQueryTemplateConfigReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryTemplateConfig"

	category "templateConfig"

	desc """查询模板中的具体配置"""

	rest {
		request {
			url "GET /v1/template-configurations/configs"
			url "GET /v1/template-configurations/configs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryTemplateConfigMsg.class

			desc """查询模板中的具体配置"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryTemplateConfigReply.class
		}
	}
}