package org.zstack.templateConfig

import org.zstack.templateConfig.APIApplyTemplateConfigEvent

doc {
	title "ApplyTemplateConfig"

	category "templateConfig"

	desc """应用模板,批量设置对应的GlobalConfig"""

	rest {
		request {
			url "PUT /v1/template-configurations/{templateUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIApplyTemplateConfigMsg.class

			desc """应用模板,批量设置对应的GlobalConfig"""

			params {

				column {
					name "templateUuid"
					enclosedIn "applyTemplateConfig"
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
			clz APIApplyTemplateConfigEvent.class
		}
	}
}