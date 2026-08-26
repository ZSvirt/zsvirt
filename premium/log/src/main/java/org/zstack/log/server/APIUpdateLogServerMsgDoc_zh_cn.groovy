package org.zstack.log.server

import org.zstack.log.server.APIUpdateLogServerEvent

doc {
	title "UpdateLogServer"

	category "logServer"

	desc """更新日志服务器"""

	rest {
		request {
			url "PUT /v1/log/servers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateLogServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateLogServer"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "name"
					enclosedIn "updateLogServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "description"
					enclosedIn "updateLogServer"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv 5.1.0"
				}
			}
		}

		response {
			clz APIUpdateLogServerEvent.class
		}
	}
}