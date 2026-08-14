package org.zstack.zops.api

import org.zstack.zops.api.APIGetChronyServersReply

doc {
	title "GetChronyServers"

	category "zops"

	desc """获取当前chrony时间源服务器"""

	rest {
		request {
			url "GET /v1/zops/chrony/servers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetChronyServersMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIGetChronyServersReply.class
		}
	}
}