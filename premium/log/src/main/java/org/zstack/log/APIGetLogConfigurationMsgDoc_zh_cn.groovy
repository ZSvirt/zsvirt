package org.zstack.log

import org.zstack.log.APIGetLogConfigurationReply

doc {
	title "GetLogConfiguration"

	category "log.configuration"

	desc """获取日志服务器配置"""

	rest {
		request {
			url "GET /v1/log/configurations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetLogConfigurationMsg.class

			desc """"""

			params {

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
			clz APIGetLogConfigurationReply.class
		}
	}
}