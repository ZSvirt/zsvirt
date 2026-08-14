package org.zstack.snmp.agent

import org.zstack.snmp.agent.APIQuerySnmpAgentReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySnmpAgent"

	category "SNMP"

	desc """查询SNMP代理"""

	rest {
		request {
			url "GET /v1/snmp/agent"
			url "GET /v1/snmp/agent/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySnmpAgentMsg.class

			desc """查询SNMP代理消息"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySnmpAgentReply.class
		}
	}
}