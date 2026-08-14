package org.zstack.log

import org.zstack.log.APIUpdateLogConfigurationEvent

doc {
	title "UpdateLogConfiguration"

	category "log.configuration"

	desc """修改日志服务器配置"""

	rest {
		request {
			url "PUT /v1/log/configurations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateLogConfigurationMsg.class

			desc """"""

			params {

				column {
					name "configId"
					enclosedIn "updateLogConfiguration"
					desc "配置的id"
					location "body"
					type "long"
					optional false
					since "3.7.0"
				}
				column {
					name "name"
					enclosedIn "updateLogConfiguration"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "description"
					enclosedIn "updateLogConfiguration"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIUpdateLogConfigurationEvent.class
		}
	}
}