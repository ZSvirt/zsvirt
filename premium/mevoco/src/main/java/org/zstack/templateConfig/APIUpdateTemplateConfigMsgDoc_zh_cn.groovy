package org.zstack.templateConfig

import org.zstack.templateConfig.APIUpdateTemplateConfigEvent

doc {
	title "UpdateTemplateConfig"

	category "templateConfig"

	desc """更新模板值"""

	rest {
		request {
			url "PUT /v1/template-configurations/{templateUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateTemplateConfigMsg.class

			desc """更新模板值"""

			params {

				column {
					name "templateUuid"
					enclosedIn "updateTemplateConfig"
					desc "模板Uuid"
					location "url"
					type "String"
					optional false
					since "3.6.0"
				}
				column {
					name "category"
					enclosedIn "updateTemplateConfig"
					desc "对应的GlobalConfig配置类型"
					location "body"
					type "String"
					optional false
					since "3.6.0"
				}
				column {
					name "name"
					enclosedIn "updateTemplateConfig"
					desc "对应的GlobalConfig配置名称"
					location "body"
					type "String"
					optional false
					since "3.6.0"
				}
				column {
					name "value"
					enclosedIn "updateTemplateConfig"
					desc "模板值"
					location "body"
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
			clz APIUpdateTemplateConfigEvent.class
		}
	}
}