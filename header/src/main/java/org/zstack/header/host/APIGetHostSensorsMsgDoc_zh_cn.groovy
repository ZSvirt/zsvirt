package org.zstack.header.host

import org.zstack.header.host.APIGetHostSensorsReply

doc {
	title "GetHostSensors"

	category "Host"

	desc """获取主机传感器信息"""

	rest {
		request {
			url "GET /v1/hosts/{uuid}/get-sensors"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostSensorsMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
			}
		}

		response {
			clz APIGetHostSensorsReply.class
		}
	}
}