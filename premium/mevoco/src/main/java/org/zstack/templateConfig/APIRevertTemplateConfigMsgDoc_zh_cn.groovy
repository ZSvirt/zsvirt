package org.zstack.templateConfig

import org.zstack.templateConfig.APIRevertTemplateConfigEvent

doc {
	title "RevertTemplateConfig"

	category "templateConfig"

	desc """重置模板配置到默认值"""

	rest {
		request {
			url "PUT /v1/template-configurations/{templateUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRevertTemplateConfigMsg.class

			desc """重置模板配置到默认值"""

			params {

				column {
					name "templateUuid"
					enclosedIn "revertTemplateConfig"
					desc "模板UUID"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APIRevertTemplateConfigEvent.class
		}
	}
}