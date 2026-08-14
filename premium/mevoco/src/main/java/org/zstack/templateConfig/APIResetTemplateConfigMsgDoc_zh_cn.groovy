package org.zstack.templateConfig

import org.zstack.templateConfig.APIResetTemplateConfigEvent

doc {
	title "ResetTemplateConfig"

	category "templateConfig"

	desc """重置模板到初始状态"""

	rest {
		request {
			url "PUT /v1/template-configurations/{templateUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIResetTemplateConfigMsg.class

			desc """重置模板到初始状态"""

			params {

				column {
					name "templateUuid"
					enclosedIn "resetTemplateConfig"
					desc "模板Uuid"
					location "url"
					type "String"
					optional false
					since "3.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
			}
		}

		response {
			clz APIResetTemplateConfigEvent.class
		}
	}
}