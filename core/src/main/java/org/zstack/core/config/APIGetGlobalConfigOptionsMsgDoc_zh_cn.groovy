package org.zstack.core.config

import org.zstack.core.config.APIGetGlobalConfigOptionsReply

doc {
	title "GetGlobalConfigOptions"

	category "globalConfig"

	desc """获取全局配置参数选项"""

	rest {
		request {
			url "GET /v1/global-configurations/{category}/{name}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetGlobalConfigOptionsMsg.class

			desc """"""

			params {

				column {
					name "category"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "3.14.0"
				}
				column {
					name "name"
					enclosedIn ""
					desc "资源名称"
					location "url"
					type "String"
					optional false
					since "3.14.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.14.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.14.0"
				}
			}
		}

		response {
			clz APIGetGlobalConfigOptionsReply.class
		}
	}
}