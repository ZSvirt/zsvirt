package org.zstack.templateConfig

import org.zstack.templateConfig.APIQueryGlobalConfigTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryGlobalConfigTemplate"

	category "templateConfig"

	desc """查询所有模板信息"""

	rest {
		request {
			url "GET /v1/template-configurations/templates"
			url "GET /v1/template-configurations/templates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryGlobalConfigTemplateMsg.class

			desc """查询所有模板信息"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryGlobalConfigTemplateReply.class
		}
	}
}