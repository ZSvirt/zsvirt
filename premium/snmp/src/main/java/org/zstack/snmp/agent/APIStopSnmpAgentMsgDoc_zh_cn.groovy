package org.zstack.snmp.agent

import org.zstack.snmp.agent.APIStopSnmpAgentEvent

doc {
	title "StopSnmpAgent"

	category "SNMP"

	desc """停止SNMP代理"""

	rest {
		request {
			url "PUT /v1/snmp/agent/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIStopSnmpAgentMsg.class

			desc """停止SNMP代理消息"""

			params {

				column {
					name "uuid"
					enclosedIn "stopSnmpAgent"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APIStopSnmpAgentEvent.class
		}
	}
}