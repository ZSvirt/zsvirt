package org.zstack.header.host

import org.zstack.header.host.APIShutdownHostEvent

doc {
	title "ShutdownHost"

	category "host"

	desc """关闭物理机"""

	rest {
		request {
			url "PUT /v1/hosts/power/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIShutdownHostMsg.class

			desc """关闭一台物理机"""

			params {

				column {
					name "uuid"
					enclosedIn "shutdownHost"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "returnEarly"
					enclosedIn "shutdownHost"
					desc "是否提前返回"
					location "body"
					type "boolean"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "force"
					enclosedIn "shutdownHost"
					desc "是否强制关机"
					location "body"
					type "boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "method"
					enclosedIn "shutdownHost"
					desc "关机方式"
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
			clz APIShutdownHostEvent.class
		}
	}
}