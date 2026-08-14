package org.zstack.log

import org.zstack.log.APIDeleteLogConfigurationEvent

doc {
	title "DeleteLogConfiguration"

	category "log.configuration"

	desc """删除日志服务器配置"""

	rest {
		request {
			url "DELETE /v1/log/configurations/log4j2"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteLogConfigurationMsg.class

			desc """"""

			params {

				column {
					name "configId"
					enclosedIn ""
					desc "配置id"
					location "query"
					type "Long"
					optional false
					since "3.7.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIDeleteLogConfigurationEvent.class
		}
	}
}