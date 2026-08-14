package org.zstack.core.config

import org.zstack.core.config.APIResetGlobalConfigEvent

doc {
	title "ResetGlobalConfig"

	category "globalConfig"

	desc """重置全局配置"""

	rest {
		request {
			url "PUT /v1/global-configurations/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIResetGlobalConfigMsg.class

			desc """重置全局配置消息"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APIResetGlobalConfigEvent.class
		}
	}
}