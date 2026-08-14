package org.zstack.header.console

import org.zstack.header.console.APIQueryConsoleProxyAgentReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryConsoleProxyAgent"

	category "console"

	desc """查询控制台代理Agent"""

	rest {
		request {
			url "GET /v1/consoles/agents"
			url "GET /v1/consoles/agents/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryConsoleProxyAgentMsg.class

			desc """查询控制台代理Agent"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryConsoleProxyAgentReply.class
		}
	}
}