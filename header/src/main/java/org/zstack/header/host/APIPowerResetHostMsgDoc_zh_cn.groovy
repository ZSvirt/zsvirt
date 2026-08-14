package org.zstack.header.host

import org.zstack.header.host.APIPowerResetHostEvent

doc {
	title "PowerResetHost"

	category "host"

	desc """重启物理机"""

	rest {
		request {
			url "PUT /v1/hosts/power/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPowerResetHostMsg.class

			desc """重启一台物理机"""

			params {

				column {
					name "uuid"
					enclosedIn "powerResetHost"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "method"
					enclosedIn "powerResetHost"
					desc "重启方式"
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
				column {
					name "returnEarly"
					enclosedIn "powerResetHost"
					desc "是否提前返回"
					location "body"
					type "boolean"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIPowerResetHostEvent.class
		}
	}
}