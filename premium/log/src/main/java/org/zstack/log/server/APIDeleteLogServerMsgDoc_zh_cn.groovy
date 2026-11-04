package org.zstack.log.server

import org.zstack.log.server.APIDeleteLogServerEvent

doc {
	title "DeleteLogSever"

	category "logServer"

	desc """删除日志服务器"""

	rest {
		request {
			url "DELETE /v1/log/servers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteLogServerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional false
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIDeleteLogServerEvent.class
		}
	}
}