package org.zstack.header.console

import org.zstack.header.console.APIUpdateConsoleProxyAgentEvent

doc {
	title "UpdateConsoleProxyAgent"

	category "console"

	desc """更新控制台代理"""

	rest {
		request {
			url "PUT /v1/consoles/agents/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateConsoleProxyAgentMsg.class

			desc """更新控制台代理"""

			params {

				column {
					name "uuid"
					enclosedIn "updateConsoleProxyAgent"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "consoleProxyOverriddenIp"
					enclosedIn "updateConsoleProxyAgent"
					desc "新的控制台代理IP"
					location "body"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 2.3"
				}
				column {
					name "consoleProxyPort"
					enclosedIn "updateConsoleProxyAgent"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "zsv 3.11.0"
				}
			}
		}

		response {
			clz APIUpdateConsoleProxyAgentEvent.class
		}
	}
}