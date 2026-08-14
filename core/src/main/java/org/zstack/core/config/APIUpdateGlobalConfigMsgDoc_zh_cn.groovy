package org.zstack.core.config

import org.zstack.core.config.APIUpdateGlobalConfigEvent

doc {
	title "UpdateGlobalConfig"

	category "globalConfig"

	desc """更新全局配置"""

	rest {
		request {
			url "PUT /v1/global-configurations/{category}/{name}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateGlobalConfigMsg.class

			desc """更新全局配置"""

			params {

				column {
					name "category"
					enclosedIn "updateGlobalConfig"
					desc "类型"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateGlobalConfig"
					desc "资源名称"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "value"
					enclosedIn "updateGlobalConfig"
					desc "值"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateGlobalConfigEvent.class
		}
	}
}