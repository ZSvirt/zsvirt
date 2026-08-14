package org.zstack.log

import org.zstack.log.APIAddLogConfigurationEvent

doc {
	title "AddLogConfiguration"

	category "log.configuration"

	desc """添加日志服务器配置"""

	rest {
		request {
			url "POST /v1/log/configurations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddLogConfigurationMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "configuration"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APIAddLogConfigurationEvent.class
		}
	}
}