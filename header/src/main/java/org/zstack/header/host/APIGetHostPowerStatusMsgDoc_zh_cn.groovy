package org.zstack.header.host

import org.zstack.header.host.APIGetHostPowerStatusEvent

doc {
	title "GetHostPowerStatus"

	category "host"

	desc """获取物理机最新电源状态"""

	rest {
		request {
			url "PUT /v1/hosts/power/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostPowerStatusMsg.class

			desc """获取一台物理机最新电源状态"""

			params {

				column {
					name "uuid"
					enclosedIn "getHostPowerStatus"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "method"
					enclosedIn "getHostPowerStatus"
					desc "获取物理机电源状态方式"
					location "body"
					type "String"
					optional true
					since "zsv 3.17.0"
					values ("AUTO","AGENT","IPMI")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIGetHostPowerStatusEvent.class
		}
	}
}